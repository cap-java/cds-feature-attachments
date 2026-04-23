/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.servicemanager;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves per-tenant object store credentials from the SAP Service Manager. Used in
 * separate-bucket multitenancy mode where each tenant has a dedicated object store instance
 * provisioned by the MTX sidecar (cap-js/attachments plugin).
 *
 * <p>This client only reads tenant bindings; instance lifecycle (subscribe/unsubscribe) is handled
 * by the cap-js sidecar.
 */
public class ServiceManagerCredentialResolver {

  private static final Logger logger =
      LoggerFactory.getLogger(ServiceManagerCredentialResolver.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 300;

  private final String smUrl;
  private final String tokenUrl;
  private final String clientId;
  private final String clientSecret;
  private final String certificate;
  private final String key;
  private final CloseableHttpClient httpClient;

  private volatile String cachedToken;
  private volatile Instant tokenExpiry = Instant.EPOCH;

  /**
   * Creates a new resolver from a {@code service-manager} service binding.
   *
   * @param smBinding the service binding for the Service Manager service
   */
  public ServiceManagerCredentialResolver(ServiceBinding smBinding) {
    requireNonNull(smBinding, "smBinding must not be null");
    Map<String, Object> creds = smBinding.getCredentials();

    this.smUrl = requireString(creds, "sm_url");
    this.clientId = requireString(creds, "clientid");
    this.clientSecret = (String) creds.get("clientsecret");
    this.certificate = (String) creds.get("certificate");
    this.key = (String) creds.get("key");

    String certUrl = (String) creds.get("certurl");
    this.tokenUrl =
        (certUrl != null && certificate != null && key != null)
            ? certUrl
            : requireString(creds, "url");

    if (clientSecret == null && (certificate == null || key == null)) {
      throw new ServiceManagerException(
          "Service Manager binding must provide either clientsecret or certificate+key for"
              + " authentication");
    }

    this.httpClient = buildHttpClient();
  }

  /**
   * Fetches the object store credentials for the given tenant from Service Manager.
   *
   * @param tenantId the tenant ID to fetch credentials for
   * @return the credentials map from the tenant's service binding
   * @throws ServiceManagerException if no binding is found or the SM API call fails
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getCredentialsForTenant(String tenantId) {
    requireNonNull(tenantId, "tenantId must not be null");
    String token = getAccessToken();

    String labelQuery = "service eq 'OBJECT_STORE' and tenant_id eq '" + tenantId + "'";
    String encodedQuery = URLEncoder.encode(labelQuery, StandardCharsets.UTF_8);
    String url = smUrl + "/v1/service_bindings?labelQuery=" + encodedQuery;

    logger.debug("Fetching object store binding for tenant {} from Service Manager", tenantId);

    HttpGet request = new HttpGet(url);
    request.setHeader("Accept", "application/json");
    request.setHeader("Authorization", "Bearer " + token);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode != 200) {
        throw new ServiceManagerException(
            "Service Manager returned status %d when fetching binding for tenant %s: %s"
                .formatted(statusCode, tenantId, body));
      }

      Map<String, Object> result = MAPPER.readValue(body, MAP_TYPE);
      List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

      if (items == null || items.isEmpty()) {
        throw new ServiceManagerException(
            "No object store service binding found for tenant %s. ".formatted(tenantId)
                + "Ensure the tenant is subscribed and the MTX sidecar has provisioned an object"
                + " store instance.");
      }

      Map<String, Object> binding = items.get(0);
      Map<String, Object> credentials = (Map<String, Object>) binding.get("credentials");

      if (credentials == null) {
        throw new ServiceManagerException(
            "Object store binding for tenant %s has no credentials".formatted(tenantId));
      }

      logger.info("Retrieved object store credentials for tenant {}", tenantId);
      return credentials;
    } catch (ServiceManagerException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceManagerException(
          "Failed to fetch object store credentials for tenant %s".formatted(tenantId), e);
    }
  }

  private synchronized String getAccessToken() {
    if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
      return cachedToken;
    }
    return fetchAccessToken();
  }

  private String fetchAccessToken() {
    boolean useMtls = certificate != null && key != null && clientSecret == null;
    String tokenEndpoint = tokenUrl + "/oauth/token";

    logger.debug(
        "Fetching Service Manager access token using {}", useMtls ? "mTLS" : "client credentials");

    HttpPost request = new HttpPost(tokenEndpoint);
    request.setHeader("Accept", "application/json");
    request.setHeader("Content-Type", "application/x-www-form-urlencoded");

    String formBody;
    if (useMtls) {
      formBody =
          "grant_type=client_credentials&response_type=token&client_id="
              + URLEncoder.encode(clientId, StandardCharsets.UTF_8);
    } else {
      formBody =
          "grant_type=client_credentials&client_id="
              + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
              + "&client_secret="
              + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);
    }

    try {
      request.setEntity(new StringEntity(formBody, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new ServiceManagerException("Failed to build token request", e);
    }

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode != 200) {
        throw new ServiceManagerException(
            "Token endpoint returned status %d: %s".formatted(statusCode, body));
      }

      Map<String, Object> tokenResponse = MAPPER.readValue(body, MAP_TYPE);
      String accessToken = (String) tokenResponse.get("access_token");
      if (accessToken == null) {
        throw new ServiceManagerException("Token response missing access_token");
      }

      Object expiresIn = tokenResponse.get("expires_in");
      long ttlSeconds =
          expiresIn instanceof Number
              ? ((Number) expiresIn).longValue() - TOKEN_EXPIRY_BUFFER_SECONDS
              : 3300;

      cachedToken = accessToken;
      tokenExpiry = Instant.now().plusSeconds(Math.max(ttlSeconds, 60));

      logger.debug("Service Manager access token acquired, expires in {} seconds", ttlSeconds);
      return accessToken;
    } catch (ServiceManagerException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceManagerException("Failed to fetch Service Manager access token", e);
    }
  }

  private CloseableHttpClient buildHttpClient() {
    boolean useMtls = certificate != null && key != null;
    if (useMtls) {
      try {
        SSLContext sslContext = createSSLContext(certificate, key);
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
        return HttpClients.custom().setSSLSocketFactory(sslSocketFactory).build();
      } catch (GeneralSecurityException | IOException e) {
        throw new ServiceManagerException(
            "Failed to create mTLS HTTP client for Service Manager", e);
      }
    }
    return HttpClients.createDefault();
  }

  private static SSLContext createSSLContext(String certificatePem, String privateKeyPem)
      throws GeneralSecurityException, IOException {
    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    Collection<? extends Certificate> certChain =
        certFactory.generateCertificates(
            new ByteArrayInputStream(certificatePem.getBytes(StandardCharsets.UTF_8)));

    PrivateKey privateKey = parsePrivateKey(privateKeyPem);

    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    keyStore.setKeyEntry("client", privateKey, new char[0], certChain.toArray(new Certificate[0]));

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, new char[0]);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, null);
    return sslContext;
  }

  private static PrivateKey parsePrivateKey(String pem) throws IOException {
    try (PEMParser parser = new PEMParser(new StringReader(pem))) {
      Object parsed = parser.readObject();
      if (parsed == null) {
        throw new IOException("PEM stream contained no recognizable key block.");
      }
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      if (parsed instanceof PEMKeyPair keyPair) {
        return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
      } else if (parsed instanceof PrivateKeyInfo keyInfo) {
        return converter.getPrivateKey(keyInfo);
      }
      throw new IOException("Unsupported PEM key format. Expected PKCS#1 or PKCS#8 private key.");
    }
  }

  private static String requireString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof String str) || str.isEmpty()) {
      throw new ServiceManagerException(
          "Service Manager binding is missing required credential: " + key);
    }
    return str;
  }
}

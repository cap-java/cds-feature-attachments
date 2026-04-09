/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides OAuth access tokens for authenticating with SAP BTP Service Manager. Caches the token
 * in-memory and refreshes it before expiry.
 */
public class ServiceManagerTokenProvider {

  private static final Logger logger = LoggerFactory.getLogger(ServiceManagerTokenProvider.class);
  private static final Duration REFRESH_MARGIN = Duration.ofMinutes(5);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ServiceManagerCredentials credentials;
  private final HttpClient httpClient;

  private String cachedToken;
  private Instant tokenExpiry = Instant.MIN;

  public ServiceManagerTokenProvider(
      ServiceManagerCredentials credentials, HttpClient httpClient) {
    this.credentials = credentials;
    this.httpClient = httpClient;
  }

  /**
   * Returns a valid access token, fetching or refreshing as needed.
   *
   * @return the OAuth access token
   * @throws ServiceManagerException if the token cannot be obtained
   */
  public synchronized String getAccessToken() {
    if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minus(REFRESH_MARGIN))) {
      return cachedToken;
    }
    return fetchToken();
  }

  private String fetchToken() {
    String tokenUrl = credentials.authUrl() + "/oauth/token";
    String body =
        "grant_type=client_credentials"
            + "&client_id="
            + URLEncoder.encode(credentials.clientId(), StandardCharsets.UTF_8)
            + "&client_secret="
            + URLEncoder.encode(credentials.clientSecret(), StandardCharsets.UTF_8);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new ServiceManagerException(
            "Failed to obtain Service Manager token: HTTP %d - %s"
                .formatted(response.statusCode(), response.body()));
      }

      JsonNode json = MAPPER.readTree(response.body());
      cachedToken = json.get("access_token").asText();
      int expiresIn = json.get("expires_in").asInt();
      tokenExpiry = Instant.now().plusSeconds(expiresIn);
      logger.debug("Obtained Service Manager token, expires in {} seconds", expiresIn);
      return cachedToken;
    } catch (IOException e) {
      throw new ServiceManagerException("Failed to obtain Service Manager token", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServiceManagerException("Interrupted while obtaining Service Manager token", e);
    }
  }
}

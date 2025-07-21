package com.sap.cds.feature.attachments.oss.handler;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;

import com.sap.cds.OSCredentials;
import com.sap.cds.OSSConstants;
import com.sap.cds.services.environment.CdsProperties;
import com.sap.cloud.environment.servicebinding.api.DefaultServiceBindingAccessor;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpClientFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.OAuth2DestinationBuilder;
import com.sap.cloud.sdk.cloudplatform.connectivity.OnBehalfOf;
import com.sap.cloud.security.config.ClientCredentials;

public class TokenHandler {
    private static final String SDM_TOKEN_ENDPOINT = "url";
    private static final String SDM_URL = "uri";
    private static final String CLIENT_ID = "clientid";
    private static final String CLIENT_SECRET = "clientsecret";
  
    private static TokenHandler instance;
  
    private TokenHandler() {}
  
    public static TokenHandler getTokenHandlerInstance() {
      if (instance == null) {
        instance = new TokenHandler();
      }
      return instance;
    }
  
    /*public byte[] toBytes(String str) {
      return requireNonNull(str).getBytes(StandardCharsets.UTF_8);
    }
  
    public String toString(byte[] bytes) {
      return new String(requireNonNull(bytes), StandardCharsets.UTF_8);
    }*/
  
    public OSCredentials getOSCredentials() {
      //Map<String, Object> uaaCredentials = getUaaCredentials();
      //Map<String, Object> uaa = (Map<String, Object>) uaaCredentials.get("uaa");
      OSCredentials osCredentials = new OSCredentials();
      //osCredentials.setBaseTokenUrl(uaa.get("url").toString());
      //osCredentials.setUrl(uaaCredentials.get("uri").toString());
      //osCredentials.setClientId(uaa.get("clientid").toString());
      //osCredentials.setClientSecret(uaa.get("clientsecret").toString());
      return osCredentials;
    }
  
    public Map<String, Object> getUaaCredentials() {
      List<ServiceBinding> allServiceBindings =
          DefaultServiceBindingAccessor.getInstance().getServiceBindings();
      ServiceBinding sdmBinding =
          allServiceBindings.stream()
              .filter(binding -> "sdm".equalsIgnoreCase(binding.getServiceName().orElse(null)))
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("SDM binding not found"));
      return sdmBinding.getCredentials();
    }
  
    public HttpClient getHttpClient(
        ServiceBinding binding,
        CdsProperties.ConnectionPool connectionPoolConfig,
        String subdomain,
        String type) {
  
      Map<String, Object> uaaCredentials;
      if (binding != null && !binding.getCredentials().isEmpty()) {
        uaaCredentials = binding.getCredentials();
      } else {
        uaaCredentials = getUaaCredentials();
      }
  
      Map<String, Object> uaa = (Map<String, Object>) uaaCredentials.get("uaa");
  
      ClientCredentials clientCredentials =
          new ClientCredentials(uaa.get(CLIENT_ID).toString(), uaa.get(CLIENT_SECRET).toString());
  
      String baseTokenUrl = uaa.get(SDM_TOKEN_ENDPOINT).toString();
      if (subdomain != null && !subdomain.isEmpty()) {
        String providerSubdomain =
            baseTokenUrl.substring(baseTokenUrl.indexOf("/") + 2, baseTokenUrl.indexOf("."));
        baseTokenUrl = baseTokenUrl.replace(providerSubdomain, subdomain);
      }
  
      DefaultHttpDestination destination;
      /*if (NAMED_USER_FLOW.equals(type)) {
        destination =
            OAuth2DestinationBuilder.forTargetUrl(uaaCredentials.get(SDM_URL).toString())
                .withTokenEndpoint(baseTokenUrl)
                .withClient(clientCredentials, OnBehalfOf.NAMED_USER_CURRENT_TENANT)
                .property(
                    OSSConstants.SDM_DESTINATION_KEY, OSSConstants.SDM_TOKEN_EXCHANGE_DESTINATION)
                .build();
      } else */{
        destination =
            OAuth2DestinationBuilder.forTargetUrl(uaaCredentials.get(SDM_URL).toString())
                .withTokenEndpoint(baseTokenUrl)
                .withClient(clientCredentials, OnBehalfOf.TECHNICAL_USER_CURRENT_TENANT)
                .property(
                    OSSConstants.SDM_DESTINATION_KEY,
                    OSSConstants.SDM_TECHNICAL_CREDENTIALS_FLOW_DESTINATION)
                .build();
      }
  
      DefaultHttpClientFactory.DefaultHttpClientFactoryBuilder builder =
          DefaultHttpClientFactory.builder();
  
      if (connectionPoolConfig == null) {
        Duration timeout = Duration.ofSeconds(OSSConstants.CONNECTION_TIMEOUT);
        builder.timeoutMilliseconds((int) timeout.toMillis());
        builder.maxConnectionsPerRoute(OSSConstants.MAX_CONNECTIONS);
        builder.maxConnectionsTotal(OSSConstants.MAX_CONNECTIONS);
      } else {
        builder.timeoutMilliseconds((int) connectionPoolConfig.getTimeout().toMillis());
        builder.maxConnectionsPerRoute(connectionPoolConfig.getMaxConnectionsPerRoute());
        builder.maxConnectionsTotal(connectionPoolConfig.getMaxConnections());
      }
  
      return builder.build().createHttpClient(destination);
    }
  }
  
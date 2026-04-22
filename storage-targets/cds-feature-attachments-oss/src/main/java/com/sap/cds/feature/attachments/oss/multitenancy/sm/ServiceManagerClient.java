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
import java.util.Map;
import java.util.Optional;

/**
 * REST client for the SAP BTP Service Manager API. Reads existing service bindings for per-tenant
 * credential resolution in separate-bucket multitenancy mode. Tenant lifecycle management
 * (onboarding/offboarding) is handled by the cap-js MTX sidecar.
 */
public class ServiceManagerClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String smUrl;
  private final ServiceManagerTokenProvider tokenProvider;
  private final HttpClient httpClient;

  public ServiceManagerClient(
      ServiceManagerCredentials credentials,
      ServiceManagerTokenProvider tokenProvider,
      HttpClient httpClient) {
    this.smUrl = credentials.smUrl();
    this.tokenProvider = tokenProvider;
    this.httpClient = httpClient;
  }

  /**
   * Looks up an existing service binding for the given tenant.
   *
   * @return the binding credentials, or empty if no binding exists
   */
  public Optional<ServiceManagerBindingResult> getBinding(String tenantId) {
    String labelQuery = "tenant_id eq '%s'".formatted(tenantId);
    String url = smUrl + "/v1/service_bindings?labelQuery=" + encode(labelQuery);
    JsonNode json = sendGet(url);
    JsonNode items = json.get("items");
    if (items == null || items.isEmpty()) {
      return Optional.empty();
    }
    JsonNode binding = items.get(0);
    String bindingId = binding.get("id").asText();
    String instanceId = binding.get("service_instance_id").asText();
    @SuppressWarnings("unchecked")
    Map<String, Object> credentials = MAPPER.convertValue(binding.get("credentials"), Map.class);
    return Optional.of(new ServiceManagerBindingResult(bindingId, instanceId, credentials));
  }

  // --- HTTP Helpers ---

  private JsonNode sendGet(String url) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
            .GET()
            .build();
    return parseJson(sendRequest(request).body());
  }

  private HttpResponse<String> sendRequest(HttpRequest request) {
    try {
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new ServiceManagerException(
          "HTTP request failed: %s %s".formatted(request.method(), request.uri()), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServiceManagerException(
          "Interrupted during HTTP request: %s %s".formatted(request.method(), request.uri()), e);
    }
  }

  private static JsonNode parseJson(String json) {
    try {
      return MAPPER.readTree(json);
    } catch (IOException e) {
      throw new ServiceManagerException("Failed to parse JSON response", e);
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /** Result of a binding lookup or creation, containing IDs and credentials. */
  public record ServiceManagerBindingResult(
      String bindingId, String instanceId, Map<String, Object> credentials) {}
}

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST client for the SAP BTP Service Manager API. Manages object store service instances and
 * bindings for per-tenant bucket provisioning (separate bucket multitenancy mode).
 */
public class ServiceManagerClient {

  private static final Logger logger = LoggerFactory.getLogger(ServiceManagerClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(5);
  static final Duration INITIAL_POLL_INTERVAL = Duration.ofSeconds(5);

  private final String smUrl;
  private final ServiceManagerTokenProvider tokenProvider;
  private final HttpClient httpClient;
  private final Duration pollTimeout;

  public ServiceManagerClient(
      ServiceManagerCredentials credentials,
      ServiceManagerTokenProvider tokenProvider,
      HttpClient httpClient) {
    this(credentials, tokenProvider, httpClient, DEFAULT_POLL_TIMEOUT);
  }

  ServiceManagerClient(
      ServiceManagerCredentials credentials,
      ServiceManagerTokenProvider tokenProvider,
      HttpClient httpClient,
      Duration pollTimeout) {
    this.smUrl = credentials.smUrl();
    this.tokenProvider = tokenProvider;
    this.httpClient = httpClient;
    this.pollTimeout = pollTimeout;
  }

  // --- Read Operations ---

  /** Returns the Service Manager offering ID for the "objectstore" service. */
  public String getOfferingId() {
    String url = smUrl + "/v1/service_offerings?fieldQuery=" + encode("name eq 'objectstore'");
    JsonNode json = sendGet(url);
    JsonNode items = json.get("items");
    if (items == null || items.isEmpty()) {
      throw new ServiceManagerException("No 'objectstore' service offering found");
    }
    return items.get(0).get("id").asText();
  }

  /** Returns the plan ID for the given offering. Tries "standard", then "s3-standard". */
  public String getPlanId(String offeringId) {
    for (String planName : new String[] {"standard", "s3-standard"}) {
      String query = "service_offering_id eq '%s' and name eq '%s'".formatted(offeringId, planName);
      String url = smUrl + "/v1/service_plans?fieldQuery=" + encode(query);
      JsonNode json = sendGet(url);
      JsonNode items = json.get("items");
      if (items != null && !items.isEmpty()) {
        return items.get(0).get("id").asText();
      }
    }
    throw new ServiceManagerException(
        "No 'standard' or 's3-standard' plan found for offering " + offeringId);
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

  /**
   * Looks up an existing service instance for the given tenant.
   *
   * @return the instance ID, or empty if none exists
   */
  public Optional<String> getInstanceByTenant(String tenantId) {
    String labelQuery = "tenant_id eq '%s'".formatted(tenantId);
    String url = smUrl + "/v1/service_instances?labelQuery=" + encode(labelQuery);
    JsonNode json = sendGet(url);
    JsonNode items = json.get("items");
    if (items == null || items.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(items.get(0).get("id").asText());
  }

  // --- Write Operations ---

  /**
   * Creates a new object store service instance for a tenant.
   *
   * @return the instance ID
   */
  public String createInstance(String tenantId, String planId) {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("name", "object-store-" + tenantId);
    body.put("service_plan_id", planId);
    ObjectNode labels = body.putObject("labels");
    ArrayNode tenantLabel = labels.putArray("tenant_id");
    tenantLabel.add(tenantId);
    ArrayNode serviceLabel = labels.putArray("service");
    serviceLabel.add("OBJECT_STORE");

    HttpResponse<String> response = sendPost(smUrl + "/v1/service_instances", body.toString());

    if (response.statusCode() == 201) {
      return parseJson(response.body()).get("id").asText();
    } else if (response.statusCode() == 202) {
      // Async creation — poll using Location header
      String location = response.headers().firstValue("Location").orElse(null);
      if (location == null) {
        throw new ServiceManagerException("Async instance creation returned no Location header");
      }
      String fullUrl = location.startsWith("http") ? location : smUrl + location;
      JsonNode result = pollUntilDone(fullUrl);
      return result.get("id").asText();
    } else {
      throw new ServiceManagerException(
          "Failed to create service instance for tenant %s: HTTP %d - %s"
              .formatted(tenantId, response.statusCode(), response.body()));
    }
  }

  /**
   * Creates a service binding for a tenant's instance.
   *
   * @return the binding credentials
   */
  @SuppressWarnings("unchecked")
  public ServiceManagerBindingResult createBinding(String tenantId, String instanceId) {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("name", "object-store-binding-" + tenantId);
    body.put("service_instance_id", instanceId);
    ObjectNode labels = body.putObject("labels");
    ArrayNode tenantLabel = labels.putArray("tenant_id");
    tenantLabel.add(tenantId);
    ArrayNode serviceLabel = labels.putArray("service");
    serviceLabel.add("OBJECT_STORE");

    HttpResponse<String> response = sendPost(smUrl + "/v1/service_bindings", body.toString());

    if (response.statusCode() != 201) {
      throw new ServiceManagerException(
          "Failed to create binding for tenant %s: HTTP %d - %s"
              .formatted(tenantId, response.statusCode(), response.body()));
    }
    JsonNode json = parseJson(response.body());
    String bindingId = json.get("id").asText();
    Map<String, Object> credentials = MAPPER.convertValue(json.get("credentials"), Map.class);
    return new ServiceManagerBindingResult(bindingId, instanceId, credentials);
  }

  /** Deletes a service binding. */
  public void deleteBinding(String bindingId) {
    HttpResponse<String> response = sendDelete(smUrl + "/v1/service_bindings/" + bindingId);
    if (response.statusCode() != 200 && response.statusCode() != 204) {
      throw new ServiceManagerException(
          "Failed to delete binding %s: HTTP %d - %s"
              .formatted(bindingId, response.statusCode(), response.body()));
    }
    logger.info("Deleted SM binding {}", bindingId);
  }

  /** Deletes a service instance. Handles async deletion with polling. */
  public void deleteInstance(String instanceId) {
    HttpResponse<String> response = sendDelete(smUrl + "/v1/service_instances/" + instanceId);
    if (response.statusCode() == 200 || response.statusCode() == 204) {
      logger.info("Deleted SM instance {}", instanceId);
    } else if (response.statusCode() == 202) {
      String location = response.headers().firstValue("Location").orElse(null);
      if (location != null) {
        String fullUrl = location.startsWith("http") ? location : smUrl + location;
        pollUntilDone(fullUrl);
      }
      logger.info("Deleted SM instance {} (async)", instanceId);
    } else {
      throw new ServiceManagerException(
          "Failed to delete instance %s: HTTP %d - %s"
              .formatted(instanceId, response.statusCode(), response.body()));
    }
  }

  // --- Polling ---

  JsonNode pollUntilDone(String url) {
    long startTime = System.currentTimeMillis();
    int iteration = 1;
    while (true) {
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed > pollTimeout.toMillis()) {
        throw new ServiceManagerException("Polling timed out after " + pollTimeout);
      }

      Duration sleepDuration = INITIAL_POLL_INTERVAL.multipliedBy(iteration);
      try {
        Thread.sleep(sleepDuration.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ServiceManagerException("Interrupted while polling", e);
      }

      JsonNode json = sendGet(url);
      String state =
          json.has("state")
              ? json.get("state").asText()
              : json.path("last_operation").path("state").asText("");
      if ("succeeded".equals(state)) {
        return json;
      } else if ("failed".equals(state)) {
        String description = json.path("last_operation").path("description").asText("unknown");
        throw new ServiceManagerException("Operation failed: " + description);
      }
      logger.debug("Polling SM operation, state={}, iteration={}", state, iteration);
      iteration++;
    }
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

  private HttpResponse<String> sendPost(String url, String body) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return sendRequest(request);
  }

  private HttpResponse<String> sendDelete(String url) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
            .DELETE()
            .build();
    return sendRequest(request);
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

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.servicemanager;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for SAP Service Manager REST API operations related to object store instance lifecycle.
 * Used in separate-bucket multitenancy mode to provision and deprovision per-tenant object store
 * instances.
 *
 * <p>Delegates token management and HTTP client to {@link ServiceManagerCredentialResolver}.
 */
public class ServiceManagerClient {

  private static final Logger logger = LoggerFactory.getLogger(ServiceManagerClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final long POLL_INTERVAL_MS = 5000;
  private static final long POLL_TIMEOUT_MS = 5 * 60 * 1000;
  private static final List<String> SUPPORTED_PLANS = List.of("standard", "s3-standard");
  private static final String LABEL_QUERY_TEMPLATE =
      "service eq 'OBJECT_STORE' and tenant_id eq '%s'";

  private final ServiceManagerCredentialResolver credentialResolver;

  public ServiceManagerClient(ServiceManagerCredentialResolver credentialResolver) {
    this.credentialResolver =
        requireNonNull(credentialResolver, "credentialResolver must not be null");
  }

  /**
   * Checks whether a service binding already exists for the given tenant.
   *
   * @param tenantId the tenant ID
   * @return true if at least one binding exists
   */
  public boolean bindingExistsForTenant(String tenantId) {
    List<Map<String, Object>> items = queryByLabel("/v1/service_bindings", tenantId);
    return !items.isEmpty();
  }

  /**
   * Retrieves the service offering ID for the "objectstore" service.
   *
   * @return the offering ID
   * @throws ServiceManagerException if not found
   */
  public String getOfferingId() {
    String url =
        credentialResolver.getSmUrl()
            + "/v1/service_offerings?fieldQuery="
            + URLEncoder.encode("name eq 'objectstore'", StandardCharsets.UTF_8);

    Map<String, Object> result = executeGet(url);
    List<Map<String, Object>> items = getItems(result);

    if (items.isEmpty()) {
      throw new ServiceManagerException(
          "Object store service offering not found in Service Manager");
    }
    return (String) items.get(0).get("id");
  }

  /**
   * Retrieves a supported service plan ID for the given offering.
   *
   * @param offeringId the service offering ID
   * @return the plan ID
   * @throws ServiceManagerException if no supported plan is found
   */
  public String getPlanId(String offeringId) {
    for (String planName : SUPPORTED_PLANS) {
      String fieldQuery =
          "service_offering_id eq '%s' and catalog_name eq '%s'".formatted(offeringId, planName);
      String url =
          credentialResolver.getSmUrl()
              + "/v1/service_plans?fieldQuery="
              + URLEncoder.encode(fieldQuery, StandardCharsets.UTF_8);

      Map<String, Object> result = executeGet(url);
      List<Map<String, Object>> items = getItems(result);

      if (!items.isEmpty()) {
        String planId = (String) items.get(0).get("id");
        logger.debug("Using object store plan '{}' with ID {}", planName, planId);
        return planId;
      }
    }
    throw new ServiceManagerException(
        "No supported object store service plan found in Service Manager. Tried: "
            + SUPPORTED_PLANS);
  }

  /**
   * Creates an object store instance for the given tenant and polls until provisioning completes.
   *
   * @param tenantId the tenant ID
   * @param planId the service plan ID
   * @return the instance ID
   * @throws ServiceManagerException if creation fails or times out
   */
  @SuppressWarnings("unchecked")
  public String createInstance(String tenantId, String planId) {
    String instanceName = "object-store-" + tenantId + "-" + UUID.randomUUID();
    Map<String, Object> body =
        Map.of(
            "name",
            instanceName,
            "service_plan_id",
            planId,
            "parameters",
            Map.of(),
            "labels",
            Map.of("tenant_id", List.of(tenantId), "service", List.of("OBJECT_STORE")));

    logger.info("Creating object store instance '{}' for tenant {}", instanceName, tenantId);

    String url = credentialResolver.getSmUrl() + "/v1/service_instances";
    Map<String, Object> response = executePost(url, body);

    String instanceId = (String) response.get("id");
    if (instanceId == null) {
      throw new ServiceManagerException(
          "Service Manager did not return an instance ID for tenant " + tenantId);
    }

    pollUntilDone(instanceId, tenantId);
    return instanceId;
  }

  /**
   * Creates a service binding for the given tenant's object store instance.
   *
   * @param tenantId the tenant ID
   * @param instanceId the instance ID to bind
   * @return the binding ID
   */
  public String createBinding(String tenantId, String instanceId) {
    String bindingName = "object-store-" + tenantId + "-" + UUID.randomUUID();
    Map<String, Object> body =
        Map.of(
            "name",
            bindingName,
            "service_instance_id",
            instanceId,
            "parameters",
            Map.of(),
            "labels",
            Map.of("tenant_id", List.of(tenantId), "service", List.of("OBJECT_STORE")));

    logger.info("Creating binding '{}' for tenant {}", bindingName, tenantId);

    String url = credentialResolver.getSmUrl() + "/v1/service_bindings";
    Map<String, Object> response = executePost(url, body);

    String bindingId = (String) response.get("id");
    if (bindingId == null) {
      throw new ServiceManagerException(
          "Service Manager did not return a binding ID for tenant " + tenantId);
    }
    return bindingId;
  }

  /**
   * Finds the binding ID for the given tenant, if one exists.
   *
   * @param tenantId the tenant ID
   * @return the binding ID, or empty if none found
   */
  public Optional<String> findBindingIdForTenant(String tenantId) {
    List<Map<String, Object>> items = queryByLabel("/v1/service_bindings", tenantId);
    if (items.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable((String) items.get(0).get("id"));
  }

  /**
   * Deletes the service binding with the given ID.
   *
   * @param bindingId the binding ID to delete
   */
  public void deleteBinding(String bindingId) {
    String url = credentialResolver.getSmUrl() + "/v1/service_bindings/" + bindingId;
    executeDelete(url);
    logger.info("Deleted service binding {}", bindingId);
  }

  /**
   * Finds the instance ID for the given tenant, if one exists.
   *
   * @param tenantId the tenant ID
   * @return the instance ID, or empty if none found
   */
  public Optional<String> findInstanceIdForTenant(String tenantId) {
    List<Map<String, Object>> items = queryByLabel("/v1/service_instances", tenantId);
    if (items.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable((String) items.get(0).get("id"));
  }

  /**
   * Deletes the service instance with the given ID.
   *
   * @param instanceId the instance ID to delete
   */
  public void deleteInstance(String instanceId) {
    String url = credentialResolver.getSmUrl() + "/v1/service_instances/" + instanceId;
    executeDelete(url);
    logger.info("Deleted service instance {}", instanceId);
  }

  private void pollUntilDone(String instanceId, String tenantId) {
    String url =
        credentialResolver.getSmUrl() + "/v1/service_instances/" + instanceId + "/last_operation";
    long startTime = System.currentTimeMillis();
    int iteration = 1;

    while (true) {
      long waitTime = POLL_INTERVAL_MS * iteration;
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ServiceManagerException(
            "Interrupted while waiting for object store instance for tenant " + tenantId, e);
      }

      Map<String, Object> operation = executeGet(url);
      String state = (String) operation.get("state");

      if ("succeeded".equals(state)) {
        logger.info("Object store instance provisioned for tenant {}", tenantId);
        return;
      }

      if ("failed".equals(state)) {
        String description = (String) operation.get("description");
        throw new ServiceManagerException(
            "Object store instance provisioning failed for tenant %s: %s"
                .formatted(tenantId, description));
      }

      if (System.currentTimeMillis() - startTime > POLL_TIMEOUT_MS) {
        throw new ServiceManagerException(
            "Timed out waiting for object store instance provisioning for tenant " + tenantId);
      }

      iteration++;
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> queryByLabel(String path, String tenantId) {
    String labelQuery = LABEL_QUERY_TEMPLATE.formatted(tenantId);
    String url =
        credentialResolver.getSmUrl()
            + path
            + "?labelQuery="
            + URLEncoder.encode(labelQuery, StandardCharsets.UTF_8);

    Map<String, Object> result = executeGet(url);
    return getItems(result);
  }

  private Map<String, Object> executeGet(String url) {
    String token = credentialResolver.getAccessToken();
    HttpGet request = new HttpGet(url);
    request.setHeader("Accept", "application/json");
    request.setHeader("Authorization", "Bearer " + token);

    try (CloseableHttpResponse response = credentialResolver.getHttpClient().execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode < 200 || statusCode >= 300) {
        throw new ServiceManagerException(
            "Service Manager GET %s returned status %d: %s".formatted(url, statusCode, body));
      }
      return MAPPER.readValue(body, MAP_TYPE);
    } catch (ServiceManagerException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceManagerException("Service Manager GET request failed: " + url, e);
    }
  }

  private Map<String, Object> executePost(String url, Map<String, Object> requestBody) {
    String token = credentialResolver.getAccessToken();
    HttpPost request = new HttpPost(url);
    request.setHeader("Accept", "application/json");
    request.setHeader("Content-Type", "application/json");
    request.setHeader("Authorization", "Bearer " + token);

    try {
      String jsonBody = MAPPER.writeValueAsString(requestBody);
      request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new ServiceManagerException("Failed to serialize request body", e);
    }

    try (CloseableHttpResponse response = credentialResolver.getHttpClient().execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

      if (statusCode < 200 || statusCode >= 300) {
        throw new ServiceManagerException(
            "Service Manager POST %s returned status %d: %s".formatted(url, statusCode, body));
      }
      return MAPPER.readValue(body, MAP_TYPE);
    } catch (ServiceManagerException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceManagerException("Service Manager POST request failed: " + url, e);
    }
  }

  private void executeDelete(String url) {
    String token = credentialResolver.getAccessToken();
    HttpDelete request = new HttpDelete(url);
    request.setHeader("Accept", "application/json");
    request.setHeader("Authorization", "Bearer " + token);

    try (CloseableHttpResponse response = credentialResolver.getHttpClient().execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode < 200 || statusCode >= 300) {
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        throw new ServiceManagerException(
            "Service Manager DELETE %s returned status %d: %s".formatted(url, statusCode, body));
      }
    } catch (ServiceManagerException e) {
      throw e;
    } catch (Exception e) {
      throw new ServiceManagerException("Service Manager DELETE request failed: " + url, e);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getItems(Map<String, Object> response) {
    Object items = response.get("items");
    if (items instanceof List<?> list) {
      return (List<Map<String, Object>>) list;
    }
    return List.of();
  }
}

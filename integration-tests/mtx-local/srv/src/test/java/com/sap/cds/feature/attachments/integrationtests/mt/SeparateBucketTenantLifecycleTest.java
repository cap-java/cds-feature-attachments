/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.attachments.integrationtests.mt.utils.SubscriptionEndpointClient;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the tenant lifecycle in separate-bucket multitenancy mode. Tests that
 * subscribing a new tenant provisions storage, the tenant can create and read attachments, and
 * unsubscribing cleans up data.
 *
 * <p>These tests target the subscribe/unsubscribe flow that will trigger the {@code
 * ObjectStoreSubscribeHandler} and {@code ObjectStoreUnsubscribeHandler} once they are implemented
 * for the separate-bucket mode.
 *
 * <p>Note: In the local test environment, the MTX sidecar manages tenant DB provisioning, while the
 * object store provisioning (via Service Manager) would be handled by the OSS plugin's lifecycle
 * handlers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-with-tenants")
class SeparateBucketTenantLifecycleTest {

  private static final String DOCUMENTS_URL = "/odata/v4/MtTestService/Documents";

  @Autowired MockMvc client;
  @Autowired ObjectMapper objectMapper;

  SubscriptionEndpointClient subscriptionEndpointClient;

  @BeforeEach
  void setup() {
    subscriptionEndpointClient = new SubscriptionEndpointClient(objectMapper, client);
  }

  // --- Subscribe and access ---

  @Test
  void subscribeTenant_thenCreateAndReadDocument() throws Exception {
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    String title = "SeparateBucket-Doc-" + UUID.randomUUID();

    // Create document in newly subscribed tenant
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-3", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + title + "\" }"))
        .andExpect(status().isCreated());

    // Read back
    String response =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-3", "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values = objectMapper.readTree(response).path("value");
    boolean found = false;
    for (JsonNode node : values) {
      if (title.equals(node.get("title").asText(""))) {
        found = true;
      }
    }
    assertThat(found).as("Created document should be visible to the tenant").isTrue();
  }

  // --- Subscribe, create data, unsubscribe, resubscribe — data should be gone ---

  @Test
  void subscribeCreateUnsubscribeResubscribe_dataIsGone() throws Exception {
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    String title = "Ephemeral-" + UUID.randomUUID();

    // Create document
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-3", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + title + "\" }"))
        .andExpect(status().isCreated());

    // Unsubscribe — should clean up data
    subscriptionEndpointClient.unsubscribeTenant("tenant-3");

    // Resubscribe — fresh tenant
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    // Read — previously created document should NOT exist
    String response =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-3", "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values = objectMapper.readTree(response).path("value");
    for (JsonNode node : values) {
      assertThat(node.get("title").asText(""))
          .as("Document from previous subscription should be cleaned up")
          .isNotEqualTo(title);
    }
  }

  // --- Unsubscribe does not affect other tenants ---

  @Test
  void unsubscribeTenant3_doesNotAffectTenant1() throws Exception {
    // Create document in tenant-1 (pre-subscribed via mock tenants)
    String t1Title = "T1-Survives-" + UUID.randomUUID();
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-1", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + t1Title + "\" }"))
        .andExpect(status().isCreated());

    // Subscribe and unsubscribe tenant-3
    subscriptionEndpointClient.subscribeTenant("tenant-3");
    subscriptionEndpointClient.unsubscribeTenant("tenant-3");

    // Tenant-1 data should still be there
    String response =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-1", "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values = objectMapper.readTree(response).path("value");
    boolean found = false;
    for (JsonNode node : values) {
      if (t1Title.equals(node.get("title").asText(""))) {
        found = true;
      }
    }
    assertThat(found).as("Tenant-1 data should survive tenant-3 unsubscription").isTrue();
  }

  // --- Multiple tenants can operate concurrently ---

  @Test
  void multipleTenantsOperateConcurrently() throws Exception {
    String t1Title = "Concurrent-T1-" + UUID.randomUUID();
    String t2Title = "Concurrent-T2-" + UUID.randomUUID();

    // Create in both tenants
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-1", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + t1Title + "\" }"))
        .andExpect(status().isCreated());

    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-2", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + t2Title + "\" }"))
        .andExpect(status().isCreated());

    // Each tenant sees only their own
    assertTenantSeesDocument("user-in-tenant-1", t1Title);
    assertTenantDoesNotSeeDocument("user-in-tenant-1", t2Title);
    assertTenantSeesDocument("user-in-tenant-2", t2Title);
    assertTenantDoesNotSeeDocument("user-in-tenant-2", t1Title);
  }

  // --- Subscribe same tenant twice (idempotent) ---

  @Test
  void subscribeSameTenantTwice_isIdempotent() throws Exception {
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    String title = "Idempotent-" + UUID.randomUUID();
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-3", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + title + "\" }"))
        .andExpect(status().isCreated());

    // Subscribe again — should not lose existing data
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    assertTenantSeesDocument("user-in-tenant-3", title);
  }

  @AfterEach
  void tearDown() {
    try {
      subscriptionEndpointClient.unsubscribeTenant("tenant-3");
    } catch (Exception ignored) {
      // best effort cleanup
    }
  }

  // --- Helper methods ---

  private void assertTenantSeesDocument(String user, String title) throws Exception {
    String response =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic(user, "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values = objectMapper.readTree(response).path("value");
    boolean found = false;
    for (JsonNode node : values) {
      if (title.equals(node.get("title").asText(""))) {
        found = true;
      }
    }
    assertThat(found).as(user + " should see document: " + title).isTrue();
  }

  private void assertTenantDoesNotSeeDocument(String user, String title) throws Exception {
    String response =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic(user, "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values = objectMapper.readTree(response).path("value");
    for (JsonNode node : values) {
      assertThat(node.get("title").asText(""))
          .as(user + " should NOT see document: " + title)
          .isNotEqualTo(title);
    }
  }
}

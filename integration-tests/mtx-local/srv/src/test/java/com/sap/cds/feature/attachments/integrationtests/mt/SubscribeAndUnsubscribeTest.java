/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.feature.attachments.integrationtests.mt.utils.SubscriptionEndpointClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-with-tenants")
class SubscribeAndUnsubscribeTest {

  private static final String DOCUMENTS_URL = "/odata/v4/MtTestService/Documents";

  @Autowired MockMvc client;
  @Autowired ObjectMapper objectMapper;

  SubscriptionEndpointClient subscriptionEndpointClient;

  @BeforeEach
  void setup() {
    subscriptionEndpointClient = new SubscriptionEndpointClient(objectMapper, client);
  }

  @Test
  void subscribeTenant3_thenServiceIsReachable() throws Exception {
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    client
        .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-3", "")))
        .andExpect(status().isOk());
  }

  @Test
  void unsubscribeTenant3_thenServiceFails() throws Exception {
    subscriptionEndpointClient.subscribeTenant("tenant-3");

    // Verify it works
    client
        .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-3", "")))
        .andExpect(status().isOk());

    subscriptionEndpointClient.unsubscribeTenant("tenant-3");

    // Service should fail after unsubscription
    client
        .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-3", "")))
        .andExpect(status().isInternalServerError());
  }

  @AfterEach
  void tearDown() {
    try {
      subscriptionEndpointClient.unsubscribeTenant("tenant-3");
    } catch (Exception ignored) {
      // best effort cleanup
    }
  }
}

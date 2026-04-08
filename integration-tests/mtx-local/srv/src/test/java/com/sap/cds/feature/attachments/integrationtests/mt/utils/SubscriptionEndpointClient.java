/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.utils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

public class SubscriptionEndpointClient {

  private static final String MT_SUBSCRIPTIONS_TENANTS = "/mt/v1.0/subscriptions/tenants/";

  private final ObjectMapper objectMapper;
  private final MockMvc client;
  private final String credentials =
      "Basic " + Base64.getEncoder().encodeToString("privileged:".getBytes(StandardCharsets.UTF_8));

  public SubscriptionEndpointClient(ObjectMapper objectMapper, MockMvc client) {
    this.objectMapper = objectMapper;
    this.client = client;
  }

  public void subscribeTenant(String tenant) throws Exception {
    SubscriptionPayload payload = new SubscriptionPayload();
    payload.subscribedTenantId = tenant;
    payload.subscribedSubdomain = tenant.concat(".sap.com");
    payload.eventType = "CREATE";

    client
        .perform(
            put(MT_SUBSCRIPTIONS_TENANTS.concat(payload.subscribedTenantId))
                .header(HttpHeaders.AUTHORIZATION, credentials)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isCreated());
  }

  public void unsubscribeTenant(String tenant) throws Exception {
    DeletePayload payload = new DeletePayload();
    payload.subscribedTenantId = tenant;

    client
        .perform(
            delete(MT_SUBSCRIPTIONS_TENANTS.concat(payload.subscribedTenantId))
                .header(HttpHeaders.AUTHORIZATION, credentials)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isNoContent());
  }

  static class SubscriptionPayload {
    public String subscribedTenantId;
    public String subscribedSubdomain;
    public String eventType;
  }

  static class DeletePayload {
    public String subscribedTenantId;
  }
}

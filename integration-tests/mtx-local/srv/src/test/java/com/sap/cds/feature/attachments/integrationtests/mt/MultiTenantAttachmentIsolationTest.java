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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-with-tenants")
class MultiTenantAttachmentIsolationTest {

  private static final String DOCUMENTS_URL = "/odata/v4/MtTestService/Documents";

  @Autowired MockMvc client;
  @Autowired ObjectMapper objectMapper;

  @Test
  void createDocumentInTenant1_notVisibleInTenant2() throws Exception {
    String uniqueTitle = "Only-in-T1-" + UUID.randomUUID();

    // Create a document in tenant-1
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-1", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + uniqueTitle + "\" }"))
        .andExpect(status().isCreated());

    // Read documents in tenant-2 — should NOT see the tenant-1 document
    String response =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-2", "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values = objectMapper.readTree(response).path("value");
    values.forEach(node -> assertThat(node.get("title").asText("")).isNotEqualTo(uniqueTitle));
  }

  @Test
  void createDocumentsInBothTenants_eachSeeOnlyOwn() throws Exception {
    String titleT1 = "Doc-T1-" + UUID.randomUUID();
    String titleT2 = "Doc-T2-" + UUID.randomUUID();

    // Create in tenant-1
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-1", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + titleT1 + "\" }"))
        .andExpect(status().isCreated());

    // Create in tenant-2
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-2", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"" + titleT2 + "\" }"))
        .andExpect(status().isCreated());

    // Read from tenant-1 — should see titleT1 but not titleT2
    String response1 =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-1", "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values1 = objectMapper.readTree(response1).path("value");
    boolean foundT1 = false;
    for (JsonNode node : values1) {
      assertThat(node.get("title").asText("")).isNotEqualTo(titleT2);
      if (titleT1.equals(node.get("title").asText(""))) {
        foundT1 = true;
      }
    }
    assertThat(foundT1).isTrue();

    // Read from tenant-2 — should see titleT2 but not titleT1
    String response2 =
        client
            .perform(get(DOCUMENTS_URL).with(httpBasic("user-in-tenant-2", "")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode values2 = objectMapper.readTree(response2).path("value");
    boolean foundT2 = false;
    for (JsonNode node : values2) {
      assertThat(node.get("title").asText("")).isNotEqualTo(titleT1);
      if (titleT2.equals(node.get("title").asText(""))) {
        foundT2 = true;
      }
    }
    assertThat(foundT2).isTrue();
  }
}

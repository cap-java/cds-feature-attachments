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
// TODO: Add tests that upload/download actual binary attachment content across tenants
//  to verify storage-level isolation (not just entity-level isolation).
class MultiTenantAttachmentIsolationTest {

  private static final String DOCUMENTS_URL = "/odata/v4/MtTestService/Documents";

  @Autowired MockMvc client;
  @Autowired ObjectMapper objectMapper;

  @Test
  void createDocumentInTenant1_notVisibleInTenant2() throws Exception {
    // Create a document in tenant-1
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-1", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"Only in tenant-1\" }"))
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
    values.forEach(
        node -> assertThat(node.get("title").asText("")).isNotEqualTo("Only in tenant-1"));
  }

  @Test
  void createDocumentsInBothTenants_eachSeeOnlyOwn() throws Exception {
    // Create in tenant-1
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-1", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"Doc-T1\" }"))
        .andExpect(status().isCreated());

    // Create in tenant-2
    client
        .perform(
            post(DOCUMENTS_URL)
                .with(httpBasic("user-in-tenant-2", ""))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"title\": \"Doc-T2\" }"))
        .andExpect(status().isCreated());

    // Read from tenant-1 — should see Doc-T1 but not Doc-T2
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
      assertThat(node.get("title").asText("")).isNotEqualTo("Doc-T2");
      if ("Doc-T1".equals(node.get("title").asText(""))) {
        foundT1 = true;
      }
    }
    assertThat(foundT1).isTrue();

    // Read from tenant-2 — should see Doc-T2 but not Doc-T1
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
      assertThat(node.get("title").asText("")).isNotEqualTo("Doc-T1");
      if ("Doc-T2".equals(node.get("title").asText(""))) {
        foundT2 = true;
      }
    }
    assertThat(foundT2).isTrue();
  }
}

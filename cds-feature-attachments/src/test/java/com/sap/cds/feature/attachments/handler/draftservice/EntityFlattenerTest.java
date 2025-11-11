/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static com.sap.cds.services.draft.Drafts.IS_ACTIVE_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import org.junit.jupiter.api.Test;

public class EntityFlattenerTest {

  @Test
  void nestedReferenceWithFilterIsFlattened() {
    // Create a nested reference with a filter on the last segment: AdminService.Books -> covers
    // (with filter)
    CqnSelect select =
        Select.from(
            "AdminService.Books",
            c -> c.to("covers").filter(e -> e.get(IS_ACTIVE_ENTITY).eq(false)));

    // The target entity name should be the flattened version
    var result = CQL.copy(select, new EntityFlattener());
    // The filter should still be there
    assertThat(result.toString())
        .contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":false}]}");
    // Should contain root entity
    assertThat(result.toString()).contains("AdminService.Books");
    // Should not contain the original nested structure
    assertThat(result.toString()).doesNotContain("\"to\":");
  }

  @Test
  void nestedReferenceWithoutFilterIsFlattened() {
    // Create a nested reference with a filter on the last segment: AdminService.Books -> covers
    // (with filter)
    CqnSelect select = Select.from("AdminService.Books", c -> c.to("covers").to("anoter_nesting"));

    // The target entity name should be the flattened version
    var result = CQL.copy(select, new EntityFlattener());
    // Should contain root entity
    assertThat(result.toString()).contains("\"ref\":[\"AdminService.Books\"]");
    System.out.println(result.toString());
    // Should not contain the original nested structure
    assertThat(result.toString()).doesNotContain("\"to\":");
  }
}

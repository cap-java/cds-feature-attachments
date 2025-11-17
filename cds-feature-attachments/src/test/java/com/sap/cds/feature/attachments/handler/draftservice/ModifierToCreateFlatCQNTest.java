/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static com.sap.cds.services.draft.Drafts.IS_ACTIVE_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import org.junit.jupiter.api.Test;

class ModifierToCreateFlatCQNTest {

  private static final String TEST_DRAFT_SERVICE_BOOKS = "test.DraftService.Books";

  @Test
  void activeEntityReplacedToFalse() {
    var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true));

    var result = CQL.copy(select, new ModifierToCreateFlatCQN(false, RootTable_.CDS_NAME));

    assertThat(result.toString())
        .contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":false}]}");
    assertThat(result.toString()).doesNotContain("true");
  }

  @Test
  void activeEntityReplacedToTrue() {
    var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(false));

    var result = CQL.copy(select, new ModifierToCreateFlatCQN(true, RootTable_.CDS_NAME));

    assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}");
    assertThat(result.toString()).doesNotContain("false");
  }

  @Test
  void entityNameReplacedAndActiveEntity() {
    var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true));

    var result =
        CQL.copy(select, new ModifierToCreateFlatCQN(true, RootTable_.CDS_NAME + "_draft"));

    // Expects the entity to have an IsActiveEntity filter in the reference
    assertThat(result.toString())
        .contains(
            "{\"id\":\"unit.test.TestService.RootTable_draft\",\"where\":[{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}");
  }

  @Test
  void entityNameNotReplacedAndActiveEntity() {
    var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true));

    var result = CQL.copy(select, new ModifierToCreateFlatCQN(true, RootTable_.CDS_NAME));

    // Expects the entity to have an IsActiveEntity filter in the reference even when entity name
    // doesn't change
    assertThat(result.toString())
        .contains(
            "{\"id\":\"unit.test.TestService.RootTable\",\"where\":[{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}");
  }

  @Test
  void selectWithFilterReplace() {
    CqnSelect select =
        Select.from(
            TEST_DRAFT_SERVICE_BOOKS,
            c ->
                c.filter(e -> e.get(IS_ACTIVE_ENTITY).eq(false))
                    .to("relatedMovies")
                    .filter(e -> e.get(IS_ACTIVE_ENTITY).eq(false))
                    .to("relatedBook"));

    var result = CQL.copy(select, new ModifierToCreateFlatCQN(true, TEST_DRAFT_SERVICE_BOOKS));

    assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}");
    assertThat(result.toString()).doesNotContain("false");
  }

  @Test
  void onlyRefActiveEntityIsReplaced() {
    var select =
        Select.from(RootTable_.class)
            .where(
                root ->
                    root.IsActiveEntity()
                        .eq(true)
                        .and(
                            root.HasActiveEntity()
                                .eq(true)
                                .and(
                                    CQL.constant(true)
                                        .eq(root.IsActiveEntity())
                                        .and(CQL.constant(true).eq(root.HasActiveEntity())))));

    var result = CQL.copy(select, new ModifierToCreateFlatCQN(false, RootTable_.CDS_NAME));

    assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":false}");
    assertThat(result.toString()).contains("{\"ref\":[\"HasActiveEntity\"]},\"=\",{\"val\":true}");
    assertThat(result.toString()).contains("{\"val\":false},\"=\",{\"ref\":[\"IsActiveEntity\"]}");
    assertThat(result.toString()).contains("{\"val\":true},\"=\",{\"ref\":[\"HasActiveEntity\"]}");
  }

  @Test
  void combinesNonIsActiveEntityFilterWithIsActiveEntityFilter() {
    // Create query with a filter on the last/target segment
    CqnSelect original =
        Select.from(
            CQL.entity(RootTable_.CDS_NAME)
                .filter(CQL.get("title").eq("Some Title")) // Filter on entity reference
            );

    ModifierToCreateFlatCQN modifier = new ModifierToCreateFlatCQN(true, RootTable_.CDS_NAME);

    var result = CQL.copy(original, modifier);

    // Should contain both the original title filter and IsActiveEntity filter
    assertThat(result.toString()).contains("\"title\"");
    assertThat(result.toString()).contains("\"Some Title\"");
    assertThat(result.toString()).contains("\"IsActiveEntity\"");
    assertThat(result.toString()).contains("\"val\":true");

    // The key assertion: should contain AND operation combining both filters
    assertThat(result.toString()).contains("\"and\"");
  }
}

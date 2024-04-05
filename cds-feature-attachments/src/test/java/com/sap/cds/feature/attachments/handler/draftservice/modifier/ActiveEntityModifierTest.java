package com.sap.cds.feature.attachments.handler.draftservice.modifier;

import static com.sap.cds.services.draft.Drafts.IS_ACTIVE_ENTITY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;

class ActiveEntityModifierTest {

	private static final String TEST_DRAFT_SERVICE_BOOKS = "test.DraftService.Books";

	@Test
	void activeEntityReplacedToFalse() {
		var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true));

		var result = CQL.copy(select, new ActiveEntityModifier(false, RootTable_.CDS_NAME));

		assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":false}]}");
		assertThat(result.toString()).doesNotContain("true");
	}

	@Test
	void activeEntityReplacedToTrue() {
		var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(false));

		var result = CQL.copy(select, new ActiveEntityModifier(true, RootTable_.CDS_NAME));

		assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}");
		assertThat(result.toString()).doesNotContain("false");
	}

	@Test
	void entityNameReplaced() {
		var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true));

		var result = CQL.copy(select, new ActiveEntityModifier(true, RootTable_.CDS_NAME + "_draft"));

		assertThat(result.toString()).contains("{\"ref\":[\"unit.test.TestService.RootTable_draft\"]}");
	}

	@Test
	void nothingReplaced() {
		var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true));

		var result = CQL.copy(select, new ActiveEntityModifier(true, RootTable_.CDS_NAME));

		assertThat(result).hasToString(select.toString());
	}

	@Test
	void selectWithFilterReplace() {
		CqnSelect select = Select.from(TEST_DRAFT_SERVICE_BOOKS, c -> c.filter(e -> e.get(IS_ACTIVE_ENTITY).eq(false))
																																																																		.to("relatedMovies")
																																																																		.filter(e -> e.get(IS_ACTIVE_ENTITY).eq(false))
																																																																		.to("relatedBook"));

		var result = CQL.copy(select, new ActiveEntityModifier(true, TEST_DRAFT_SERVICE_BOOKS));

		assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}");
		assertThat(result.toString()).doesNotContain("false");
	}

	@Test
	void onlyRefActiveEntityIsReplaced() {
		var select = Select.from(RootTable_.class).where(root -> root.IsActiveEntity().eq(true)
																																																													.and(root.HasActiveEntity().eq(true).and(CQL.constant(true)
																																																																																																								.eq(root.IsActiveEntity())
																																																																																																								.and(CQL.constant(true)
																																																																																																															.eq(root.HasActiveEntity())))));

		var result = CQL.copy(select, new ActiveEntityModifier(false, RootTable_.CDS_NAME));

		assertThat(result.toString()).contains("{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":false}");
		assertThat(result.toString()).contains("{\"ref\":[\"HasActiveEntity\"]},\"=\",{\"val\":true}");
		assertThat(result.toString()).contains("{\"val\":false},\"=\",{\"ref\":[\"IsActiveEntity\"]}");
		assertThat(result.toString()).contains("{\"val\":true},\"=\",{\"ref\":[\"HasActiveEntity\"]}");
	}

}

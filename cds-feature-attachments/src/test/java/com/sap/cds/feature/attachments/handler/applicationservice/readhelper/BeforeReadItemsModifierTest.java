/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnSelectListItem;

class BeforeReadItemsModifierTest {

	private BeforeReadItemsModifier cut;

	@Test
	void expandSelectExtendsContentId() {
		CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID,
				root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand(Attachment_::content)));

		cut = new BeforeReadItemsModifier(List.of("attachments"));
		runTestForExpand(cut, select, 1);
	}

	@Test
	void expandSelectNotExtendIfAssociationNotInNameMap() {
		CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID,
				root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand(Attachment_::content)));

		cut = new BeforeReadItemsModifier(List.of("test"));
		runTestForExpand(cut, select, 0);
	}

	@Test
	void expandSelectDoNotExtendContentIdIfAlreadyExist() {
		CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.itemTable()
				.expand(Items_::ID,	item -> item.attachments().expand(Attachment_::content, Attachment_::contentId)));

		cut = new BeforeReadItemsModifier(List.of("attachments"));
		runTestForExpand(cut, select, 1);
	}

	@Test
	void expandSelectDoNotExtendContentIdIfNoContentFieldIncluded() {
		CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID,
				root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand(Attachment_::ID)));

		cut = new BeforeReadItemsModifier(List.of("attachments"));
		runTestForExpand(cut, select, 0);
	}

	@Test
	void expandSelectDoNotExtendContentIdIfNoFieldIncluded() {
		CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID,
				root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand()));

		cut = new BeforeReadItemsModifier(List.of("attachments"));
		runTestForExpand(cut, select, 0);
	}

	@Test
	void expandSelectDoNotExtendIfNoFieldInWrongAssociation() {
		CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID,
				root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand()));

		cut = new BeforeReadItemsModifier(List.of("items"));
		runTestForExpand(cut, select, 0);
	}

	@Test
	void directSelectExtendsContentId() {
		CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::ID, Attachment_::content);

		runTestForDirectSelect(select, 1);
	}

	@Test
	void directSelectDoesNotExtendIfNoContentFieldSelected() {
		CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::ID);

		runTestForDirectSelect(select, 0);
	}

	@Test
	void directSelectDoesNotExtendForSelectAll() {
		CqnSelect select = Select.from(Attachment_.class);

		runTestForDirectSelect(select, 0);
	}

	@Test
	void directSelectDoesNotAddAdditionalContentId() {
		CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::content, Attachment_::contentId);

		runTestForDirectSelect(select, 1);
	}

	private void runTestForExpand(BeforeReadItemsModifier cut, CqnSelect select, int expectedFieldCount) {
		List<CqnSelectListItem> resultItems = cut.items(select.items());

		var rootExpandedItem = resultItems.stream().filter(CqnSelectListItem::isExpand).findAny().orElseThrow();
		var itemExpandedItem = rootExpandedItem.asExpand().items().stream().filter(CqnSelectListItem::isExpand).findAny()
				.orElseThrow();
		var count = itemExpandedItem.asExpand().items().stream().filter(
				item -> item.isRef() && item.asRef().displayName().equals(Attachments.CONTENT_ID)).count();
		assertThat(count).isEqualTo(expectedFieldCount);
	}

	private void runTestForDirectSelect(CqnSelect select, int expectedFieldCount) {
		cut = new BeforeReadItemsModifier(List.of(""));
		List<CqnSelectListItem> resultItems = cut.items(select.items());

		var count = resultItems.stream().filter(
				item -> item.isRef() && item.asRef().displayName().equals(Attachments.CONTENT_ID)).count();
		assertThat(count).isEqualTo(expectedFieldCount);
	}

}

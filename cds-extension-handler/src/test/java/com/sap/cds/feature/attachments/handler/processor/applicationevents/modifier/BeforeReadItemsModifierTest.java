package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.DocumentFieldNames;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnSelectListItem;

class BeforeReadItemsModifierTest {

		private BeforeReadItemsModifier cut;

		@Test
		void expandSelectExtendsDocumentId() {
				CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, item -> item.attachments().expand(Attachment_::content)));
				var fieldName = new DocumentFieldNames("content", "documentId");

				cut = new BeforeReadItemsModifier(Map.of("attachments", fieldName));
				runTestForExpand(cut, fieldName.documentIdFieldName(), select, 1);
		}

		@Test
		void expandSelectNotExtendIfAssociationNotInNameMap() {
				CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, item -> item.attachments().expand(Attachment_::content)));
				var fieldName = new DocumentFieldNames("content", "documentId");

				cut = new BeforeReadItemsModifier(Map.of("test", fieldName));
				runTestForExpand(cut, fieldName.documentIdFieldName(), select, 0);
		}

		@Test
		void expandSelectDoNotExtendDocumentIdIfAlreadyExist() {
				CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, item -> item.attachments().expand(Attachment_::content, Attachment_::documentId)));
				var fieldName = new DocumentFieldNames("content", "documentId");

				cut = new BeforeReadItemsModifier(Map.of("attachments", fieldName));
				runTestForExpand(cut, fieldName.documentIdFieldName(), select, 1);
		}

		@Test
		void expandSelectDoNotExtendDocumentIdIfNoContentFieldIncluded() {
				CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, item -> item.attachments().expand(Attachment_::ID)));
				var fieldName = new DocumentFieldNames("content", "documentId");

				cut = new BeforeReadItemsModifier(Map.of("attachments", fieldName));
				runTestForExpand(cut, fieldName.documentIdFieldName(), select, 0);
		}

		@Test
		void expandSelectDoNotExtendDocumentIdIfNoFieldIncluded() {
				CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, item -> item.attachments().expand()));
				var fieldName = new DocumentFieldNames("content", "documentId");

				cut = new BeforeReadItemsModifier(Map.of("attachments", fieldName));
				runTestForExpand(cut, fieldName.documentIdFieldName(), select, 0);
		}

		@Test
		void expandSelectDoNotExtendIfNoFieldInWrongAssociation() {
				CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, item -> item.attachments().expand()));
				var fieldName = new DocumentFieldNames("content", "documentId");

				cut = new BeforeReadItemsModifier(Map.of("items", fieldName));
				runTestForExpand(cut, fieldName.documentIdFieldName(), select, 0);
		}

		@Test
		void directSelectExtendsDocumentId() {
				CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::ID, Attachment_::content);
				var fieldName = new DocumentFieldNames("content", "documentId");

				runTestForDirectSelect(fieldName, select, 1);
		}

		@Test
		void directSelectDoesNotExtendIfNoContentFieldSelected() {
				CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::ID);
				var fieldName = new DocumentFieldNames("content", "documentId");

				runTestForDirectSelect(fieldName, select, 0);
		}

		@Test
		void directSelectDoesNotExtendIfWrongFieldName() {
				CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::ID, Attachment_::content);
				var fieldName = new DocumentFieldNames("some other content", "some other documentId");

				runTestForDirectSelect(fieldName, select, 0);
		}

		@Test
		void directSelectDoesNotExtendForSelectAll() {
				CqnSelect select = Select.from(Attachment_.class);
				var fieldName = new DocumentFieldNames("content", "documentId");

				runTestForDirectSelect(fieldName, select, 0);
		}

		@Test
		void directSelectDoesNotAddAdditionalDocumentId() {
				CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::content, Attachment_::documentId);
				var fieldName = new DocumentFieldNames("content", "documentId");

				runTestForDirectSelect(fieldName, select, 1);
		}

		private void runTestForExpand(BeforeReadItemsModifier cut, String documentIdField, CqnSelect select, int expectedFieldCount) {
				List<CqnSelectListItem> resultItems = cut.items(select.items());

				var rootExpandedItem = resultItems.stream().filter(CqnSelectListItem::isExpand).findAny().orElseThrow();
				var itemExpandedItem = rootExpandedItem.asExpand().items().stream().filter(CqnSelectListItem::isExpand).findAny().orElseThrow();
				var count = itemExpandedItem.asExpand().items().stream().filter(item -> item.isRef() && item.asRef().displayName().equals(documentIdField)).count();
				assertThat(count).isEqualTo(expectedFieldCount);
		}

		private void runTestForDirectSelect(DocumentFieldNames fieldName, CqnSelect select, int expectedFieldCount) {
				cut = new BeforeReadItemsModifier(Map.of("", fieldName));
				List<CqnSelectListItem> resultItems = cut.items(select.items());

				var count = resultItems.stream().filter(item -> item.isRef() && item.asRef().displayName().equals(fieldName.documentIdFieldName())).count();
				assertThat(count).isEqualTo(expectedFieldCount);
		}

}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class BeforeReadItemsModifierTest {

  private BeforeReadItemsModifier cut;

  @Test
  void expandSelectExtendsContentId() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                root ->
                    root.itemTable()
                        .expand(
                            Items_::ID, item -> item.attachments().expand(Attachment_::content)));

    cut = new BeforeReadItemsModifier(List.of("attachments"));
    runTestForExpand(cut, select, 1);
    runTestForExpandScannedAt(cut, select, 1);
  }

  @Test
  void expandSelectNotExtendIfAssociationNotInNameMap() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                root ->
                    root.itemTable()
                        .expand(
                            Items_::ID, item -> item.attachments().expand(Attachment_::content)));

    cut = new BeforeReadItemsModifier(List.of("test"));
    runTestForExpand(cut, select, 0);
  }

  @Test
  void expandSelectDoNotExtendContentIdIfAlreadyExist() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                root ->
                    root.itemTable()
                        .expand(
                            Items_::ID,
                            item ->
                                item.attachments()
                                    .expand(Attachment_::content, Attachment_::contentId)));

    cut = new BeforeReadItemsModifier(List.of("attachments"));
    runTestForExpand(cut, select, 1);
  }

  @Test
  void expandSelectDoNotExtendContentIdIfNoContentFieldIncluded() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                root ->
                    root.itemTable()
                        .expand(Items_::ID, item -> item.attachments().expand(Attachment_::ID)));

    cut = new BeforeReadItemsModifier(List.of("attachments"));
    runTestForExpand(cut, select, 0);
  }

  @Test
  void expandSelectDoNotExtendContentIdIfNoFieldIncluded() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand()));

    cut = new BeforeReadItemsModifier(List.of("attachments"));
    runTestForExpand(cut, select, 0);
  }

  @Test
  void expandSelectDoNotExtendIfNoFieldInWrongAssociation() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                root -> root.itemTable().expand(Items_::ID, item -> item.attachments().expand()));

    cut = new BeforeReadItemsModifier(List.of("items"));
    runTestForExpand(cut, select, 0);
  }

  @Test
  void directSelectExtendsContentId() {
    CqnSelect select =
        Select.from(Attachment_.class).columns(Attachment_::ID, Attachment_::content);

    runTestForDirectSelect(select, 1);
    runTestForDirectSelectScannedAt(select, 1);
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
    CqnSelect select =
        Select.from(Attachment_.class).columns(Attachment_::content, Attachment_::contentId);

    runTestForDirectSelect(select, 1);
  }

  private void runTestForExpand(
      BeforeReadItemsModifier cut, CqnSelect select, int expectedFieldCount) {
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var rootExpandedItem =
        resultItems.stream().filter(CqnSelectListItem::isExpand).findAny().orElseThrow();
    var itemExpandedItem =
        rootExpandedItem.asExpand().items().stream()
            .filter(CqnSelectListItem::isExpand)
            .findAny()
            .orElseThrow();
    var count =
        itemExpandedItem.asExpand().items().stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().equals(Attachments.CONTENT_ID))
            .count();
    assertThat(count).isEqualTo(expectedFieldCount);
  }

  private void runTestForDirectSelect(CqnSelect select, int expectedFieldCount) {
    cut = new BeforeReadItemsModifier(List.of(""));
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var count =
        resultItems.stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().equals(Attachments.CONTENT_ID))
            .count();
    assertThat(count).isEqualTo(expectedFieldCount);
  }

  private void runTestForExpandScannedAt(
      BeforeReadItemsModifier cut, CqnSelect select, int expectedFieldCount) {
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var rootExpandedItem =
        resultItems.stream().filter(CqnSelectListItem::isExpand).findAny().orElseThrow();
    var itemExpandedItem =
        rootExpandedItem.asExpand().items().stream()
            .filter(CqnSelectListItem::isExpand)
            .findAny()
            .orElseThrow();
    var count =
        itemExpandedItem.asExpand().items().stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().equals(Attachments.SCANNED_AT))
            .count();
    assertThat(count).isEqualTo(expectedFieldCount);
  }

  private void runTestForDirectSelectScannedAt(CqnSelect select, int expectedFieldCount) {
    cut = new BeforeReadItemsModifier(List.of(""));
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var count =
        resultItems.stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().equals(Attachments.SCANNED_AT))
            .count();
    assertThat(count).isEqualTo(expectedFieldCount);
  }

  // --- Inline attachment modifier tests ---

  @Test
  void inlineAttachmentFieldsAreAdded() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(RootTable_::ID, RootTable_::title, b -> b.get("profilePicture_content"));

    cut = new BeforeReadItemsModifier(List.of(), List.of("profilePicture"));
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var contentIdCount =
        resultItems.stream()
            .filter(
                item ->
                    item.isRef() && item.asRef().displayName().equals("profilePicture_contentId"))
            .count();
    var statusCount =
        resultItems.stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().equals("profilePicture_status"))
            .count();
    var scannedAtCount =
        resultItems.stream()
            .filter(
                item ->
                    item.isRef() && item.asRef().displayName().equals("profilePicture_scannedAt"))
            .count();
    assertThat(contentIdCount).isEqualTo(1);
    assertThat(statusCount).isEqualTo(1);
    assertThat(scannedAtCount).isEqualTo(1);
  }

  @Test
  void inlineAttachmentFieldsNotDuplicatedIfAlreadyPresent() {
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(RootTable_::ID, b -> b.get("profilePicture_contentId"));

    cut = new BeforeReadItemsModifier(List.of(), List.of("profilePicture"));
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var contentIdCount =
        resultItems.stream()
            .filter(
                item ->
                    item.isRef() && item.asRef().displayName().equals("profilePicture_contentId"))
            .count();
    assertThat(contentIdCount).isEqualTo(1);
  }

  @Test
  void emptyInlinePrefixesDoNotAddFields() {
    CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, RootTable_::title);

    cut = new BeforeReadItemsModifier(List.of(), List.of());
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var inlineFieldCount =
        resultItems.stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().startsWith("profilePicture_"))
            .count();
    assertThat(inlineFieldCount).isEqualTo(0);
  }

  @Test
  void inlineAttachmentFieldsNotAddedWithoutContentInSelect() {
    // When profilePicture_content is NOT in the select (e.g. SELECT ID, title),
    // the modifier must NOT add profilePicture_contentId/status. Otherwise it
    // would convert a SELECT * into a partial column list, breaking draftPrepare.
    CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, RootTable_::title);

    cut = new BeforeReadItemsModifier(List.of(), List.of("profilePicture"));
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    var inlineFieldCount =
        resultItems.stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().startsWith("profilePicture_"))
            .count();
    assertThat(inlineFieldCount).isEqualTo(0);
  }

  @Test
  void inlineFieldsNotAddedWhenContentIdAlreadySelected() {
    // Both profilePicture_content AND profilePicture_contentId are explicitly selected.
    // The modifier should NOT add duplicate contentId/status/scannedAt fields.
    CqnSelect select =
        Select.from(RootTable_.class)
            .columns(
                RootTable_::ID,
                b -> b.get("profilePicture_content"),
                b -> b.get("profilePicture_contentId"));

    cut = new BeforeReadItemsModifier(List.of(), List.of("profilePicture"));
    List<CqnSelectListItem> resultItems = cut.items(select.items());

    // contentId already in select, so it should appear exactly once (no duplicate added)
    var contentIdCount =
        resultItems.stream()
            .filter(
                item ->
                    item.isRef() && item.asRef().displayName().equals("profilePicture_contentId"))
            .count();
    assertThat(contentIdCount).isEqualTo(1);
    // status and scannedAt should NOT be added either since the guard prevents it
    var statusCount =
        resultItems.stream()
            .filter(
                item -> item.isRef() && item.asRef().displayName().equals("profilePicture_status"))
            .count();
    assertThat(statusCount).isEqualTo(0);
  }
}

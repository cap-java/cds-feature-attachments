/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnlyTable_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReadonlyDataContextEnhancerTest {

  private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";

  private static CdsRuntime runtime;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @Test
  void preserveReadonlyFields_isDraft_backupCreated() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();

    var attachment = Attachments.create();
    attachment.setContentId("doc-123");
    attachment.setStatus("Clean");
    Instant scannedAt = Instant.parse("2024-06-01T12:00:00Z");
    attachment.setScannedAt(scannedAt);
    attachment.setContent(null);

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(attachment), true);

    assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNotNull();
    var backup = (CdsData) attachment.get(DRAFT_READONLY_CONTEXT);
    assertThat(backup)
        .containsEntry(Attachments.CONTENT_ID, "doc-123")
        .containsEntry(Attachments.STATUS, "Clean")
        .containsEntry(Attachments.SCANNED_AT, scannedAt)
        .doesNotContainKey(Attachments.CONTENT);
  }

  @Test
  void preserveReadonlyFields_isNotDraft_backupRemoved() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();

    var attachment = Attachments.create();
    attachment.setContentId("doc-456");
    attachment.setContent(null);
    var existingBackup = CdsData.create();
    existingBackup.put(Attachments.CONTENT_ID, "old-id");
    existingBackup.put(Attachments.STATUS, "old-status");
    existingBackup.put(Attachments.SCANNED_AT, Instant.EPOCH);
    attachment.put(DRAFT_READONLY_CONTEXT, existingBackup);

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(attachment), false);

    assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNull();
  }

  @Test
  void preserveReadonlyFields_isDraft_noAttachmentEntity_nothingHappens() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Events_.CDS_NAME).orElseThrow();

    var data = CdsData.create();
    data.put("content", "some text");

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), true);

    assertThat(data.get(DRAFT_READONLY_CONTEXT)).isNull();
  }

  @Test
  void restoreReadonlyFields_withBackup_fieldsRestoredAndBackupRemoved() {
    var data = CdsData.create();
    var backup = CdsData.create();
    backup.put(Attachments.CONTENT_ID, "restored-id");
    backup.put(Attachments.STATUS, "Infected");
    Instant scannedAt = Instant.parse("2025-01-15T08:30:00Z");
    backup.put(Attachments.SCANNED_AT, scannedAt);
    data.put(DRAFT_READONLY_CONTEXT, backup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get(Attachments.CONTENT_ID)).isEqualTo("restored-id");
    assertThat(data.get(Attachments.STATUS)).isEqualTo("Infected");
    assertThat(data.get(Attachments.SCANNED_AT)).isEqualTo(scannedAt);
    assertThat(data.get(DRAFT_READONLY_CONTEXT)).isNull();
  }

  @Test
  void restoreReadonlyFields_withoutBackup_noOp() {
    var data = CdsData.create();
    data.put("someKey", "someValue");

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data).containsEntry("someKey", "someValue");
    assertThat(data).doesNotContainKey(DRAFT_READONLY_CONTEXT);
    assertThat(data).doesNotContainKey(Attachments.CONTENT_ID);
    assertThat(data).doesNotContainKey(Attachments.STATUS);
    assertThat(data).doesNotContainKey(Attachments.SCANNED_AT);
  }

  @Test
  void restoreReadonlyFields_withPartialBackup_nullsOverwriteExistingValues() {
    var data = CdsData.create();
    data.put(Attachments.STATUS, "Clean");
    var backup = CdsData.create();
    backup.put(Attachments.CONTENT_ID, "restored-id");
    // STATUS and SCANNED_AT intentionally absent from backup
    data.put(DRAFT_READONLY_CONTEXT, backup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get(Attachments.CONTENT_ID)).isEqualTo("restored-id");
    assertThat(data.get(Attachments.STATUS)).isNull();
    assertThat(data.get(Attachments.SCANNED_AT)).isNull();
    assertThat(data.get(DRAFT_READONLY_CONTEXT)).isNull();
  }

  @Test
  void preserveReadonlyFields_isNotDraft_noExistingBackup_nothingHappens() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    var attachment = Attachments.create();
    attachment.setContent(null);

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(attachment), false);

    assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNull();
  }

  @Test
  void preserveInlineReadonlyFields_isDraft_backupsInlineFields() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put("avatar_contentId", "doc-inline-123");
    row.put("avatar_status", "Clean");
    Instant scannedAt = Instant.parse("2024-06-01T12:00:00Z");
    row.put("avatar_scannedAt", scannedAt);

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), true);

    assertThat(row.get("INLINE_READONLY_CONTEXT")).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> backup = (Map<String, Object>) row.get("INLINE_READONLY_CONTEXT");
    assertThat(backup)
        .containsEntry("avatar_contentId", "doc-inline-123")
        .containsEntry("avatar_status", "Clean")
        .containsEntry("avatar_scannedAt", scannedAt);
  }

  @Test
  void preserveInlineReadonlyFields_isNotDraft_removesBackup() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put("INLINE_READONLY_CONTEXT", Map.of("avatar_contentId", "old-id"));

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), false);

    assertThat(row.get("INLINE_READONLY_CONTEXT")).isNull();
  }

  @Test
  void preserveInlineReadonlyFields_noInlinePrefixes_doesNothing() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Items_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "row-1");

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), true);

    assertThat(row.get("INLINE_READONLY_CONTEXT")).isNull();
  }

  @Test
  void preserveInlineReadonlyFields_mediaEntity_doesNothing() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "row-1");

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), true);

    assertThat(row.get("INLINE_READONLY_CONTEXT")).isNull();
  }

  @Test
  void preserveInlineReadonlyFields_partialFieldsPresent_onlyBackupsExisting() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put("avatar_contentId", "doc-partial");
    // status and scannedAt not present

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), true);

    @SuppressWarnings("unchecked")
    Map<String, Object> backup = (Map<String, Object>) row.get("INLINE_READONLY_CONTEXT");
    assertThat(backup)
        .containsEntry("avatar_contentId", "doc-partial")
        .doesNotContainKey("avatar_status")
        .doesNotContainKey("avatar_scannedAt");
  }

  @Test
  void preserveInlineReadonlyFields_noFieldsPresent_noBackupCreated() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put("title", "Test");

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), true);

    assertThat(row.get("INLINE_READONLY_CONTEXT")).isNull();
  }

  @Test
  void restoreInlineReadonlyFields_withBackup_restoresAndRemovesContext() {
    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put(
        "INLINE_READONLY_CONTEXT",
        Map.of("avatar_contentId", "restored-cid", "avatar_status", "Clean"));

    ReadonlyDataContextEnhancer.restoreInlineReadonlyFields(List.of(row));

    assertThat(row.get("avatar_contentId")).isEqualTo("restored-cid");
    assertThat(row.get("avatar_status")).isEqualTo("Clean");
    assertThat(row.get("INLINE_READONLY_CONTEXT")).isNull();
  }

  @Test
  void restoreInlineReadonlyFields_withoutBackup_noOp() {
    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put("title", "Test");

    ReadonlyDataContextEnhancer.restoreInlineReadonlyFields(List.of(row));

    assertThat(row).containsEntry("ID", "row-1");
    assertThat(row).containsEntry("title", "Test");
    assertThat(row).doesNotContainKey("INLINE_READONLY_CONTEXT");
  }

  @Test
  void restoreInlineReadonlyFields_nonMapBackup_ignored() {
    var row = CdsData.create();
    row.put("ID", "row-1");
    row.put("INLINE_READONLY_CONTEXT", "not-a-map");

    ReadonlyDataContextEnhancer.restoreInlineReadonlyFields(List.of(row));

    // The non-map value should remain, as the instanceof check fails
    assertThat(row.get("INLINE_READONLY_CONTEXT")).isEqualTo("not-a-map");
  }

  @Test
  void preserveInlineReadonlyFields_rootTableWithInline_backupsProfilePicture() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", "root-1");
    row.put("profilePicture_contentId", "pp-cid");
    row.put("profilePicture_status", "Infected");

    ReadonlyDataContextEnhancer.preserveInlineReadonlyFields(entity, List.of(row), true);

    @SuppressWarnings("unchecked")
    Map<String, Object> backup = (Map<String, Object>) row.get("INLINE_READONLY_CONTEXT");
    assertThat(backup)
        .containsEntry("profilePicture_contentId", "pp-cid")
        .containsEntry("profilePicture_status", "Infected");
  }
}

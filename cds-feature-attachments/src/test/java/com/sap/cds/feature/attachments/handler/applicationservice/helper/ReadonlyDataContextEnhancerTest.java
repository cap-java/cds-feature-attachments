/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.runtime.CdsRuntime;
import java.time.Instant;
import java.util.List;
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
}

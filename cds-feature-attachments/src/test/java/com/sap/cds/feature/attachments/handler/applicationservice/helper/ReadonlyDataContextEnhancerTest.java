/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
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

  private CdsEntity getRootTableEntity() {
    return runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
  }

  private CdsEntity getAttachmentEntity() {
    return runtime
        .getCdsModel()
        .findEntity("unit.test.TestService.RootTable.attachments")
        .orElseThrow();
  }

  // --- Composition-based preserve/restore tests ---

  @Test
  void preserveReadonlyFieldsForDraftComposition() {
    CdsEntity entity = getAttachmentEntity();
    CdsData data = CdsData.create();
    data.put(Attachments.CONTENT, new ByteArrayInputStream(new byte[0]));
    data.put(Attachments.CONTENT_ID, "cid-123");
    data.put(Attachments.STATUS, "Clean");
    Instant now = Instant.now();
    data.put(Attachments.SCANNED_AT, now);

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), true);

    CdsData backup = (CdsData) data.get(DRAFT_READONLY_CONTEXT);
    assertThat(backup).isNotNull();
    assertThat(backup.get(Attachments.CONTENT_ID)).isEqualTo("cid-123");
    assertThat(backup.get(Attachments.STATUS)).isEqualTo("Clean");
    assertThat(backup.get(Attachments.SCANNED_AT)).isEqualTo(now);
  }

  @Test
  void preserveReadonlyFieldsNonDraftRemovesContext() {
    CdsEntity entity = getAttachmentEntity();
    CdsData data = CdsData.create();
    data.put(Attachments.CONTENT, new ByteArrayInputStream(new byte[0]));
    data.put(Attachments.CONTENT_ID, "cid-123");
    data.put(DRAFT_READONLY_CONTEXT, Attachments.create());

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), false);

    assertThat(data.containsKey(DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void restoreReadonlyFieldsComposition() {
    CdsData data = CdsData.create();
    Attachments backup = Attachments.create();
    backup.setContentId("cid-restored");
    backup.setStatus("Scanning");
    Instant scannedAt = Instant.now();
    backup.setScannedAt(scannedAt);
    data.put(DRAFT_READONLY_CONTEXT, backup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get(Attachments.CONTENT_ID)).isEqualTo("cid-restored");
    assertThat(data.get(Attachments.STATUS)).isEqualTo("Scanning");
    assertThat(data.get(Attachments.SCANNED_AT)).isEqualTo(scannedAt);
    assertThat(data.containsKey(DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void restoreReadonlyFieldsNoBackupDoesNothing() {
    CdsData data = CdsData.create();
    data.put("ID", "123");

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get("ID")).isEqualTo("123");
    assertThat(data).hasSize(1);
  }

  // --- Edge-case tests from main ---

  @Test
  void preserveReadonlyFields_isDraft_noAttachmentEntity_nothingHappens() {
    CdsEntity entity = runtime.getCdsModel().findEntity(Events_.CDS_NAME).orElseThrow();

    var data = CdsData.create();
    data.put("content", "some text");

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), true);

    assertThat(data.get(DRAFT_READONLY_CONTEXT)).isNull();
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

  // --- Inline attachment preserve/restore tests ---

  @Test
  void preserveReadonlyFieldsForDraftInlineAttachment() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    CdsData data = CdsData.create();
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_contentId", "inline-cid");
    data.put("profilePicture_status", "Clean");
    Instant now = Instant.now();
    data.put("profilePicture_scannedAt", now);
    data.put("profilePicture_fileName", "photo.jpg");

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), true);

    CdsData backup = (CdsData) data.get("profilePicture_" + DRAFT_READONLY_CONTEXT);
    assertThat(backup).isNotNull();
    assertThat(backup.get(Attachments.CONTENT_ID)).isEqualTo("inline-cid");
    assertThat(backup.get(Attachments.STATUS)).isEqualTo("Clean");
    assertThat(backup.get(Attachments.SCANNED_AT)).isEqualTo(now);
    assertThat(backup.get(MediaData.FILE_NAME)).isEqualTo("photo.jpg");
  }

  @Test
  void restoreReadonlyFieldsForInlineAttachment() {
    CdsData data = CdsData.create();
    Attachments backup = Attachments.create();
    backup.setContentId("inline-restored-cid");
    backup.setStatus("Scanning");
    Instant scannedAt = Instant.now();
    backup.setScannedAt(scannedAt);
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, backup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get("profilePicture_contentId")).isEqualTo("inline-restored-cid");
    assertThat(data.get("profilePicture_status")).isEqualTo("Scanning");
    assertThat(data.get("profilePicture_scannedAt")).isEqualTo(scannedAt);
    assertThat(data.containsKey("profilePicture_" + DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void restoreReadonlyFieldsForInlineAttachmentWithFileName() {
    CdsData data = CdsData.create();
    Attachments backup = Attachments.create();
    backup.setContentId("inline-cid");
    backup.setStatus("Clean");
    backup.setFileName("preserved-file.pdf");
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, backup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get("profilePicture_contentId")).isEqualTo("inline-cid");
    assertThat(data.get("profilePicture_fileName")).isEqualTo("preserved-file.pdf");
    assertThat(data.containsKey("profilePicture_" + DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void preserveReadonlyFieldsNonDraftRemovesInlinePrefixedContext() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    CdsData data = CdsData.create();
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, Attachments.create());

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), false);

    assertThat(data.containsKey("profilePicture_" + DRAFT_READONLY_CONTEXT)).isFalse();
  }
}

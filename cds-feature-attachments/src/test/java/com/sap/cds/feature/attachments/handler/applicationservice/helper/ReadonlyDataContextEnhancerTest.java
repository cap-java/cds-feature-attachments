/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
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

  private static CdsRuntime runtime;
  private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";

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

  // --- Inline attachment preserve/restore tests ---

  @Test
  void preserveReadonlyFieldsForDraftInline() {
    CdsEntity entity = getRootTableEntity();
    CdsData data = CdsData.create();
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_contentId", "cid-inline-456");
    data.put("profilePicture_status", "Unscanned");
    Instant now = Instant.now();
    data.put("profilePicture_scannedAt", now);

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), true);

    CdsData backup = (CdsData) data.get("profilePicture_" + DRAFT_READONLY_CONTEXT);
    assertThat(backup).isNotNull();
    assertThat(backup.get(Attachments.CONTENT_ID)).isEqualTo("cid-inline-456");
    assertThat(backup.get(Attachments.STATUS)).isEqualTo("Unscanned");
    assertThat(backup.get(Attachments.SCANNED_AT)).isEqualTo(now);
  }

  @Test
  void preserveReadonlyFieldsNonDraftRemovesInlineContext() {
    CdsEntity entity = getRootTableEntity();
    CdsData data = CdsData.create();
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, Attachments.create());

    ReadonlyDataContextEnhancer.preserveReadonlyFields(entity, List.of(data), false);

    assertThat(data.containsKey("profilePicture_" + DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void restoreReadonlyFieldsInline() {
    CdsData data = CdsData.create();
    data.put("ID", "123");
    Attachments backup = Attachments.create();
    backup.setContentId("cid-inline-restored");
    backup.setStatus("Clean");
    Instant scannedAt = Instant.now();
    backup.setScannedAt(scannedAt);
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, backup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    assertThat(data.get("profilePicture_contentId")).isEqualTo("cid-inline-restored");
    assertThat(data.get("profilePicture_status")).isEqualTo("Clean");
    assertThat(data.get("profilePicture_scannedAt")).isEqualTo(scannedAt);
    assertThat(data.containsKey("profilePicture_" + DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void restoreReadonlyFieldsBothCompositionAndInline() {
    CdsData data = CdsData.create();

    // Composition backup
    Attachments compositionBackup = Attachments.create();
    compositionBackup.setContentId("cid-comp");
    compositionBackup.setStatus("Clean");
    data.put(DRAFT_READONLY_CONTEXT, compositionBackup);

    // Inline backup
    Attachments inlineBackup = Attachments.create();
    inlineBackup.setContentId("cid-inline");
    inlineBackup.setStatus("Scanning");
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, inlineBackup);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    // Composition restored
    assertThat(data.get(Attachments.CONTENT_ID)).isEqualTo("cid-comp");
    assertThat(data.get(Attachments.STATUS)).isEqualTo("Clean");
    // Inline restored
    assertThat(data.get("profilePicture_contentId")).isEqualTo("cid-inline");
    assertThat(data.get("profilePicture_status")).isEqualTo("Scanning");
    // Backup keys removed
    assertThat(data.containsKey(DRAFT_READONLY_CONTEXT)).isFalse();
    assertThat(data.containsKey("profilePicture_" + DRAFT_READONLY_CONTEXT)).isFalse();
  }

  @Test
  void restoreReadonlyFieldsInlineWithNullBackupDoesNothing() {
    CdsData data = CdsData.create();
    data.put("ID", "123");
    // Inline backup key exists but value is null
    data.put("profilePicture_" + DRAFT_READONLY_CONTEXT, null);

    ReadonlyDataContextEnhancer.restoreReadonlyFields(data);

    // null backup is skipped, key remains
    assertThat(data.get("ID")).isEqualTo("123");
    assertThat(data.containsKey("profilePicture_contentId")).isFalse();
  }
}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.EventItems_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnly_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApplicationHandlerHelperTest {

  private static CdsRuntime runtime;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @Test
  void keysAreInData() {
    Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
    var data = CdsData.create();
    data.put("key1", "value1");
    data.put("key2", "value2");
    data.put("data", "value3");
    var result = ApplicationHandlerHelper.areKeysInData(keys, data);

    assertThat(result).isTrue();
  }

  @Test
  void keyMissingInData() {
    Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
    var data = CdsData.create();
    data.put("key1", "value1");
    data.put("data", "value3");
    var result = ApplicationHandlerHelper.areKeysInData(keys, data);

    assertThat(result).isFalse();
  }

  @Test
  void keyHasWrongValue() {
    Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
    var data = CdsData.create();
    data.put("key1", "value1");
    data.put("key2", "wrong value");
    data.put("data", "value3");
    var result = ApplicationHandlerHelper.areKeysInData(keys, data);

    assertThat(result).isFalse();
  }

  @Test
  void removeDraftKey() {
    Map<String, Object> keys = Map.of("key1", "value1", "IsActiveEntity", "true");
    assertTrue(keys.containsKey("IsActiveEntity"));

    Map<String, Object> result = ApplicationHandlerHelper.removeDraftKey(keys);
    assertFalse(result.containsKey("IsActiveEntity"));
    assertTrue(result.containsKey("key1"));
  }

  @Test
  void getInlineAttachmentFieldNamesReturnsPrefix() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    List<String> fieldNames = ApplicationHandlerHelper.getInlineAttachmentFieldNames(entity);

    assertThat(fieldNames).contains("profilePicture");
  }

  @Test
  void getInlineAttachmentFieldNamesReturnsEmptyForNonInlineEntity() {
    CdsEntity entity = runtime.getCdsModel().findEntity(EventItems_.CDS_NAME).orElseThrow();
    List<String> fieldNames = ApplicationHandlerHelper.getInlineAttachmentFieldNames(entity);

    assertThat(fieldNames).isEmpty();
  }

  @Test
  void getInlineAttachmentPrefixReturnsMatchingPrefix() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    Optional<String> prefix =
        ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, "profilePicture_content");

    assertThat(prefix).isPresent().hasValue("profilePicture");
  }

  @Test
  void getInlineAttachmentPrefixReturnsEmptyForNonMatchingElement() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    Optional<String> prefix = ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, "title");

    assertThat(prefix).isEmpty();
  }

  @Test
  void condenseAttachmentsExtractsInlineAttachments() {
    CdsEntity entity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    var data = CdsData.create();
    data.put(RootTable.PROFILE_PICTURE_CONTENT_ID, "inline-cid-1");
    data.put(RootTable.PROFILE_PICTURE_STATUS, "Clean");
    data.put(RootTable.PROFILE_PICTURE_CONTENT, null);

    List<Attachments> result = ApplicationHandlerHelper.condenseAttachments(List.of(data), entity);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContentId()).isEqualTo("inline-cid-1");
    assertThat(result.get(0).get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER))
        .isEqualTo("profilePicture");
  }

  @Test
  void condenseAttachmentsDedupsByContentId() {
    CdsEntity entity = runtime.getCdsModel().findEntity(InlineOnly_.CDS_NAME).orElseThrow();
    var data = CdsData.create();
    data.put("avatar_contentId", "same-cid");
    data.put("avatar_content", null);

    List<Attachments> result = ApplicationHandlerHelper.condenseAttachments(List.of(data), entity);

    assertThat(result).hasSize(1);
  }

  @Test
  void extractInlineAttachmentExtractsPrefixedFields() {
    Map<String, Object> parentValues =
        Map.of(
            "profilePicture_contentId", "cid-123",
            "profilePicture_status", "Clean",
            "title", "test root");

    Attachments attachment =
        ApplicationHandlerHelper.extractInlineAttachment(parentValues, "profilePicture");

    assertThat(attachment.getContentId()).isEqualTo("cid-123");
    assertThat(attachment.getStatus()).isEqualTo("Clean");
    assertThat(attachment.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER))
        .isEqualTo("profilePicture");
    assertThat(attachment.containsKey("title")).isFalse();
  }
}

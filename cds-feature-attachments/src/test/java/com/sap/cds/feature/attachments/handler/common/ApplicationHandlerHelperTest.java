/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

  // --- Inline attachment tests ---

  private CdsEntity getRootTableEntity() {
    return runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
  }

  private CdsEntity getAttachmentEntity() {
    return runtime
        .getCdsModel()
        .findEntity("unit.test.TestService.RootTable.attachments")
        .orElseThrow();
  }

  @Test
  void hasInlineAttachmentElementsReturnsTrueForEntityWithInlineField() {
    var entity = getRootTableEntity();
    assertThat(ApplicationHandlerHelper.hasInlineAttachmentElements(entity)).isTrue();
  }

  @Test
  void hasInlineAttachmentElementsReturnsFalseForAttachmentEntity() {
    var entity = getAttachmentEntity();
    assertThat(ApplicationHandlerHelper.hasInlineAttachmentElements(entity)).isFalse();
  }

  @Test
  void getInlineAttachmentFieldNamesReturnsCorrectPrefixes() {
    var entity = getRootTableEntity();
    List<String> prefixes = ApplicationHandlerHelper.getInlineAttachmentFieldNames(entity);
    assertThat(prefixes).containsExactly("profilePicture");
  }

  @Test
  void getInlineAttachmentFieldNamesReturnsEmptyForAttachmentEntity() {
    var entity = getAttachmentEntity();
    List<String> prefixes = ApplicationHandlerHelper.getInlineAttachmentFieldNames(entity);
    assertThat(prefixes).isEmpty();
  }

  @Test
  void isMediaEntityReturnsTrueForEntityWithInlineAttachment() {
    var entity = getRootTableEntity();
    assertThat(ApplicationHandlerHelper.isMediaEntity(entity)).isTrue();
  }

  @Test
  void isMediaEntityReturnsTrueForDirectMediaEntity() {
    var entity = getAttachmentEntity();
    assertThat(ApplicationHandlerHelper.isMediaEntity(entity)).isTrue();
  }

  @Test
  void isDirectMediaEntityReturnsFalseForEntityWithOnlyInlineAttachments() {
    var entity = getRootTableEntity();
    assertThat(ApplicationHandlerHelper.isDirectMediaEntity(entity)).isFalse();
  }

  @Test
  void isDirectMediaEntityReturnsTrueForAttachmentEntity() {
    var entity = getAttachmentEntity();
    assertThat(ApplicationHandlerHelper.isDirectMediaEntity(entity)).isTrue();
  }

  @Test
  void isInlineAttachmentContentFieldReturnsTrueForPrefixedContent() {
    var entity = getRootTableEntity();
    var contentElement = entity.findElement("profilePicture_content").orElseThrow();
    assertThat(ApplicationHandlerHelper.isInlineAttachmentContentField(entity, contentElement))
        .isTrue();
  }

  @Test
  void isInlineAttachmentContentFieldReturnsFalseForNonContentField() {
    var entity = getRootTableEntity();
    var mimeTypeElement = entity.findElement("profilePicture_mimeType").orElseThrow();
    assertThat(ApplicationHandlerHelper.isInlineAttachmentContentField(entity, mimeTypeElement))
        .isFalse();
  }

  @Test
  void isInlineAttachmentContentFieldReturnsFalseForRegularField() {
    var entity = getRootTableEntity();
    var titleElement = entity.findElement("title").orElseThrow();
    assertThat(ApplicationHandlerHelper.isInlineAttachmentContentField(entity, titleElement))
        .isFalse();
  }

  @Test
  void getInlineAttachmentPrefixReturnsPrefixForFlattenedField() {
    var entity = getRootTableEntity();
    Optional<String> prefix =
        ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, "profilePicture_content");
    assertThat(prefix).isPresent().contains("profilePicture");
  }

  @Test
  void getInlineAttachmentPrefixReturnsPrefixForContentIdField() {
    var entity = getRootTableEntity();
    Optional<String> prefix =
        ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, "profilePicture_contentId");
    assertThat(prefix).isPresent().contains("profilePicture");
  }

  @Test
  void getInlineAttachmentPrefixReturnsEmptyForRegularField() {
    var entity = getRootTableEntity();
    Optional<String> prefix = ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, "title");
    assertThat(prefix).isEmpty();
  }

  @Test
  void getInlineAttachmentPrefixReturnsEmptyForUnprefixedContentId() {
    var entity = getRootTableEntity();
    Optional<String> prefix =
        ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, "contentId");
    assertThat(prefix).isEmpty();
  }

  @Test
  void extractInlineAttachmentStripsPrefix() {
    Map<String, Object> parentValues =
        Map.of(
            "ID", "123",
            "title", "Test",
            "profilePicture_contentId", "cid-abc",
            "profilePicture_mimeType", "image/png",
            "profilePicture_fileName", "photo.png",
            "profilePicture_status", "Clean");

    Attachments result =
        ApplicationHandlerHelper.extractInlineAttachment(parentValues, "profilePicture");

    assertThat(result.getContentId()).isEqualTo("cid-abc");
    assertThat(result.getMimeType()).isEqualTo("image/png");
    assertThat(result.getFileName()).isEqualTo("photo.png");
    assertThat(result.getStatus()).isEqualTo("Clean");
    // Non-prefixed fields should NOT be included
    assertThat(result.get("ID")).isNull();
    assertThat(result.get("title")).isNull();
  }

  @Test
  void condenseAttachmentsIncludesInlineAttachments() {
    var entity = getRootTableEntity();
    var data = CdsData.create();
    data.put("ID", "123");
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_contentId", "cid-inline");
    data.put("profilePicture_mimeType", "image/png");
    data.put("profilePicture_status", "Clean");

    List<Attachments> result = ApplicationHandlerHelper.condenseAttachments(List.of(data), entity);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContentId()).isEqualTo("cid-inline");
    assertThat(result.get(0).getMimeType()).isEqualTo("image/png");
  }

  @Test
  void condenseAttachmentsAvoidsDuplicateInlineEntries() {
    var entity = getRootTableEntity();
    var data = CdsData.create();
    data.put("ID", "123");
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_contentId", "cid-inline");
    data.put("profilePicture_status", "Clean");

    // Same data twice — condenseAttachments should deduplicate by contentId
    List<Attachments> result =
        ApplicationHandlerHelper.condenseAttachments(List.of(data, data), entity);

    long distinctContentIds = result.stream().map(Attachments::getContentId).distinct().count();
    assertThat(distinctContentIds).isEqualTo(1);
  }

  @Test
  void containsContentFieldReturnsTrueForInlineContent() {
    var entity = getRootTableEntity();
    var data = CdsData.create();
    data.put("profilePicture_content", new ByteArrayInputStream(new byte[0]));

    assertThat(ApplicationHandlerHelper.containsContentField(entity, List.of(data))).isTrue();
  }

  @Test
  void containsContentFieldReturnsFalseForNoContent() {
    var entity = getRootTableEntity();
    var data = CdsData.create();
    data.put("ID", "123");
    data.put("title", "Test");

    assertThat(ApplicationHandlerHelper.containsContentField(entity, List.of(data))).isFalse();
  }

  @Test
  void mediaContentFilterMatchesInlineContentField() {
    var entity = getRootTableEntity();
    var data = CdsData.create();
    data.put("profilePicture_content", (InputStream) new ByteArrayInputStream(new byte[0]));
    data.put("profilePicture_contentId", "cid-123");

    // Use containsContentField which internally uses MEDIA_CONTENT_FILTER
    assertThat(ApplicationHandlerHelper.containsContentField(entity, List.of(data))).isTrue();
  }
}

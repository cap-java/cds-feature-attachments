/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsStructuredType;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates how to address one attachment's fields regardless of whether it resides in a
 * composition child entity (composition-based) or is flattened into the parent entity (inline).
 *
 * <p>The plugin processes one attachment at a time. {@code AttachmentContext} describes how to read
 * and write that attachment's fields in the data map, and how to match it against existing
 * attachment data.
 */
public sealed interface AttachmentContext {

  /**
   * Resolves a logical attachment field name (e.g. {@code "contentId"}) to the actual key in the
   * data map (e.g. {@code "avatar_contentId"} for inline, or {@code "contentId"} for composition).
   */
  String fieldName(String logicalName);

  /**
   * Returns {@code true} if the given existing attachment record corresponds to this context's
   * attachment.
   *
   * @param existing the existing attachment data to check
   * @param parentKeys the parent entity keys from the current path
   */
  boolean matches(Attachments existing, Map<String, Object> parentKeys);

  /** Returns {@code true} if this is an inline attachment (flattened into the parent entity). */
  boolean isInline();

  /**
   * Determines the correct {@link AttachmentContext} from a {@code CdsDataProcessor} callback.
   *
   * @param entity the entity type at the current processing path
   * @param element the element that matched the media content filter
   * @return the appropriate context implementation
   */
  static AttachmentContext from(CdsStructuredType entity, CdsElement element) {
    Optional<String> prefix =
        ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, element.getName());
    return prefix.<AttachmentContext>map(Inline::new).orElseGet(Composition::new);
  }

  /** Context for composition-based attachments where the attachment is its own entity. */
  record Composition() implements AttachmentContext {

    @Override
    public String fieldName(String logicalName) {
      return logicalName;
    }

    @Override
    public boolean matches(Attachments existing, Map<String, Object> parentKeys) {
      return ApplicationHandlerHelper.areKeysInData(parentKeys, existing);
    }

    @Override
    public boolean isInline() {
      return false;
    }
  }

  /**
   * Context for inline attachments where the structured type fields are flattened into the parent
   * entity with a prefix (e.g. {@code "avatar_content"}, {@code "avatar_contentId"}).
   */
  record Inline(String prefix) implements AttachmentContext {

    @Override
    public String fieldName(String logicalName) {
      return prefix + "_" + logicalName;
    }

    @Override
    public boolean matches(Attachments existing, Map<String, Object> parentKeys) {
      String existingPrefix = (String) existing.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER);
      return prefix.equals(existingPrefix);
    }

    @Override
    public boolean isInline() {
      return true;
    }
  }
}

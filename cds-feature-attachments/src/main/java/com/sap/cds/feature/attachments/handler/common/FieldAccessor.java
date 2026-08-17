/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.draft.Drafts;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates how to address one attachment's fields regardless of whether it resides in a
 * composition child entity (composition-based) or is flattened into the parent entity (inline).
 *
 * <p>The plugin processes one attachment at a time. {@code FieldAccessor} describes how to read and
 * write that attachment's fields in the data map, and how to match it against existing attachment
 * data.
 */
public sealed interface FieldAccessor {

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
   * @param keys the entity keys from the current path (attachment's own keys for composition,
   *     parent entity keys for inline)
   */
  boolean matches(Attachments existing, Map<String, Object> keys);

  /** Returns {@code true} if this is an inline attachment (flattened into the parent entity). */
  boolean isInline();

  /**
   * Extracts an {@link Attachments} view from the given data map. For composition-based
   * attachments, wraps the values directly. For inline attachments, strips the prefix from flat
   * keys to produce a logical attachment view.
   *
   * @param values the data map (either the attachment entity row or the parent entity row)
   * @return an {@link Attachments} object with logical field names
   */
  Attachments extractFrom(Map<String, Object> values);

  /**
   * Determines the correct {@link FieldAccessor} from a {@code CdsDataProcessor} callback.
   *
   * @param entity the entity type at the current processing path
   * @param element the element that matched the media content filter
   * @return the appropriate context implementation
   */
  static FieldAccessor from(CdsStructuredType entity, CdsElement element) {
    Optional<String> prefix =
        ApplicationHandlerHelper.getInlineAttachmentPrefix(entity, element.getName());
    return prefix.<FieldAccessor>map(Inline::new).orElseGet(Composition::new);
  }

  /** Context for composition-based attachments where the attachment is its own entity. */
  final class Composition implements FieldAccessor {

    @Override
    public String fieldName(String logicalName) {
      return logicalName;
    }

    @Override
    public boolean matches(Attachments existing, Map<String, Object> keys) {
      return ApplicationHandlerHelper.areKeysInData(keys, existing);
    }

    @Override
    public boolean isInline() {
      return false;
    }

    @Override
    public Attachments extractFrom(Map<String, Object> values) {
      return Attachments.of(values);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Composition;
    }

    @Override
    public int hashCode() {
      return Composition.class.hashCode();
    }
  }

  /**
   * Context for inline attachments where the structured type fields are flattened into the parent
   * entity with a prefix (e.g. {@code "avatar_content"}, {@code "avatar_contentId"}).
   */
  final class Inline implements FieldAccessor {

    private final String prefix;

    public Inline(String prefix) {
      this.prefix = requireNonNull(prefix, "prefix must not be null");
    }

    public String prefix() {
      return prefix;
    }

    @Override
    public String fieldName(String logicalName) {
      return prefix + "_" + logicalName;
    }

    @Override
    public boolean matches(Attachments existing, Map<String, Object> keys) {
      String existingPrefix = (String) existing.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER);
      return prefix.equals(existingPrefix);
    }

    @Override
    public boolean isInline() {
      return true;
    }

    @Override
    public Attachments extractFrom(Map<String, Object> values) {
      Attachments attachment = ApplicationHandlerHelper.extractInlineAttachment(values, prefix);
      Object hasActiveEntity = values.get(Drafts.HAS_ACTIVE_ENTITY);
      if (hasActiveEntity != null) {
        attachment.put(Drafts.HAS_ACTIVE_ENTITY, hasActiveEntity);
      }
      return attachment;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Inline other && prefix.equals(other.prefix);
    }

    @Override
    public int hashCode() {
      return prefix.hashCode();
    }
  }
}

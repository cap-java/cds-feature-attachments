/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for detecting and reading inline attachment fields. Inline attachments are
 * structured type fields (e.g. {@code avatar : Attachment}) that get flattened by the CDS compiler
 * into prefixed columns (e.g. {@code avatar_content}, {@code avatar_contentId}).
 */
public final class InlineAttachmentHelper {

  private static final String ANNOTATION_IS_MEDIA_DATA = "_is_media_data";
  private static final String ANNOTATION_CORE_MEDIA_TYPE = "Core.MediaType";
  private static final String CONTENT_SUFFIX = "_content";

  /**
   * Finds the prefixes of inline attachment elements on the given entity. An inline attachment is
   * identified by a flat element named {@code prefix_content} that carries both the
   * {@code @_is_media_data} and {@code @Core.MediaType} annotations, where the entity itself is not
   * a media entity (i.e. not annotated with {@code @_is_media_data}).
   *
   * @param entity the entity to inspect
   * @return a list of inline attachment prefixes (e.g. {@code ["avatar", "profilePicture"]})
   */
  public static List<String> findInlineAttachmentPrefixes(CdsEntity entity) {
    if (ApplicationHandlerHelper.isMediaEntity(entity)) {
      return List.of();
    }
    List<String> prefixes = new ArrayList<>();
    entity
        .elements()
        .forEach(
            element -> {
              String name = element.getName();
              if (name.endsWith(CONTENT_SUFFIX)
                  && element.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)
                  && element.findAnnotation(ANNOTATION_CORE_MEDIA_TYPE).isPresent()) {
                String prefix = name.substring(0, name.length() - CONTENT_SUFFIX.length());
                prefixes.add(prefix);
              }
            });
    return prefixes;
  }

  /**
   * Returns whether the entity has any inline attachment elements.
   *
   * @param entity the entity to check
   * @return {@code true} if inline attachment prefixes exist
   */
  public static boolean hasInlineAttachments(CdsEntity entity) {
    return !findInlineAttachmentPrefixes(entity).isEmpty();
  }

  /**
   * Reads the current state (contentId, status, scannedAt) of inline attachment fields from the
   * database. The returned list contains one {@link Attachments} per inline attachment per row,
   * with fields mapped to unprefixed names (e.g. {@code avatar_contentId} becomes {@code
   * contentId}).
   *
   * @param persistence the persistence service
   * @param entity the entity to query
   * @param statement the filterable statement providing the entity ref and where clause
   * @param prefixes the inline attachment prefixes to read
   * @return a list of {@link Attachments} with unprefixed field names and entity keys
   */
  public static List<Attachments> readInlineAttachmentState(
      PersistenceService persistence,
      CdsEntity entity,
      CqnFilterableStatement statement,
      List<String> prefixes) {
    if (prefixes.isEmpty()) {
      return List.of();
    }

    List<com.sap.cds.ql.cqn.CqnSelectListItem> columns = new ArrayList<>();
    entity.keyElements().forEach(key -> columns.add(CQL.get(key.getName())));
    for (String prefix : prefixes) {
      columns.add(CQL.get(buildInlineFieldName(prefix, Attachments.CONTENT_ID)));
      columns.add(CQL.get(buildInlineFieldName(prefix, Attachments.STATUS)));
      columns.add(CQL.get(buildInlineFieldName(prefix, Attachments.SCANNED_AT)));
    }

    Select<?> select = Select.from(statement.ref()).columns(columns);
    statement.where().ifPresent(select::where);

    Result result = persistence.run(select);

    List<Attachments> attachments = new ArrayList<>();
    result.forEach(
        row -> {
          Map<String, Object> keys = new HashMap<>();
          entity.keyElements().forEach(key -> keys.put(key.getName(), row.get(key.getName())));

          for (String prefix : prefixes) {
            Attachments att = Attachments.create();
            att.putAll(keys);
            att.setContentId(
                (String) row.get(buildInlineFieldName(prefix, Attachments.CONTENT_ID)));
            att.setStatus((String) row.get(buildInlineFieldName(prefix, Attachments.STATUS)));
            Object scannedAt = row.get(buildInlineFieldName(prefix, Attachments.SCANNED_AT));
            if (scannedAt instanceof java.time.Instant instant) {
              att.setScannedAt(instant);
            }
            attachments.add(att);
          }
        });

    return attachments;
  }

  /**
   * Builds a flattened inline field name from the element prefix and the field name.
   *
   * @param prefix the inline attachment prefix (e.g. {@code "avatar"})
   * @param fieldName the attachment field name (e.g. {@code "contentId"})
   * @return the combined name (e.g. {@code "avatar_contentId"})
   */
  public static String buildInlineFieldName(String prefix, String fieldName) {
    return prefix + "_" + fieldName;
  }

  /**
   * Extracts the inline attachment prefix from a flattened element name that ends with {@code
   * _content}.
   *
   * @param elementName the flattened element name (e.g. {@code "avatar_content"})
   * @return the prefix (e.g. {@code "avatar"})
   */
  static String extractPrefix(String elementName) {
    return elementName.substring(0, elementName.length() - CONTENT_SUFFIX.length());
  }

  /**
   * Returns the element names of all inline attachment content fields on the given entity, as
   * identified by {@link #findInlineAttachmentPrefixes}.
   *
   * @param entity the entity to inspect
   * @return element names like {@code ["avatar_content", "profilePicture_content"]}
   */
  public static List<String> findInlineContentElements(CdsEntity entity) {
    return findInlineAttachmentPrefixes(entity).stream()
        .map(prefix -> buildInlineFieldName(prefix, "content"))
        .collect(Collectors.toList());
  }

  private InlineAttachmentHelper() {
    // avoid instantiation
  }
}

/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static java.util.Objects.nonNull;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.draft.Drafts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The class {@link ApplicationHandlerHelper} provides helper methods for the attachment application
 * handlers.
 */
public final class ApplicationHandlerHelper {
  private static final String ANNOTATION_IS_MEDIA_DATA = "_is_media_data";
  private static final String ANNOTATION_CORE_MEDIA_TYPE = "Core.MediaType";

  /**
   * A filter for media content fields. The filter checks if the entity is a media entity and if the
   * element has the annotation "Core.MediaType". Also supports inline attachment type fields where
   * the structured type is flattened into the parent entity.
   */
  public static final Filter MEDIA_CONTENT_FILTER =
      (path, element, type) -> {
        // Case 1: Composition-based attachment entity (existing behavior)
        if (path.target().type().getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)
            && element.findAnnotation(ANNOTATION_CORE_MEDIA_TYPE).isPresent()) {
          return true;
        }
        // Case 2: Inline attachment type field (flattened into parent entity)
        return isInlineAttachmentContentField(path.target().type(), element);
      };

  /**
   * Checks if the data contains a content field.
   *
   * @param entity The {@link CdsEntity entity} type of the given the data to check
   * @param data The data to check
   * @return <code>true</code> if the data contains a content field, <code>false</code> otherwise
   */
  public static boolean containsContentField(CdsEntity entity, List<? extends CdsData> data) {
    AtomicBoolean isIncluded = new AtomicBoolean();
    CdsDataProcessor.create()
        .addValidator(MEDIA_CONTENT_FILTER, (path, element, value) -> isIncluded.set(true))
        .process(data, entity);
    return isIncluded.get();
  }

  /**
   * Checks if the entity is a media entity. A media entity is an entity that is annotated with the
   * annotation "_is_media_data", or has inline structured type elements with that annotation.
   *
   * @param baseEntity The entity to check
   * @return <code>true</code> if the entity is a media entity, <code>false</code> otherwise
   */
  public static boolean isMediaEntity(CdsStructuredType baseEntity) {
    if (baseEntity.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)) {
      return true;
    }
    return hasInlineAttachmentElements(baseEntity);
  }

  /**
   * Checks if the entity is directly annotated as a media entity (without considering inline
   * elements). Used for composition-based attachment detection.
   *
   * @param baseEntity The entity to check
   * @return <code>true</code> if the entity itself has the annotation
   */
  public static boolean isDirectMediaEntity(CdsStructuredType baseEntity) {
    return baseEntity.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false);
  }

  /**
   * Checks if the entity has inline attachment elements. In the flattened CDS model, these appear
   * as elements with the annotation "_is_media_data" on the element itself, where the entity is not
   * directly annotated as a media entity. The flattened element names follow the pattern
   * "prefix_content", "prefix_contentId", etc.
   *
   * @param entity The entity to check
   * @return <code>true</code> if inline attachment elements exist
   */
  public static boolean hasInlineAttachmentElements(CdsStructuredType entity) {
    if (entity.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)) {
      return false; // Entity itself is a media entity (composition-based), not inline
    }
    return !getInlineAttachmentFieldNames(entity).isEmpty();
  }

  /**
   * Returns the inline attachment element name prefixes for a given entity. In the flattened CDS
   * model, inline attachment fields appear as "prefix_content", "prefix_contentId", etc. with
   * element-level "_is_media_data" annotation. This method finds all unique prefixes by looking for
   * elements ending with "_content" that have the annotation.
   *
   * @param entity The entity to inspect
   * @return list of inline attachment field name prefixes (e.g. ["profilePicture"])
   */
  public static List<String> getInlineAttachmentFieldNames(CdsStructuredType entity) {
    var elements = entity.elements();
    if (elements == null) return List.of();
    String contentSuffix = "_content";
    LinkedHashSet<String> fieldNames = new LinkedHashSet<>();
    elements
        .filter(e -> e.getName().endsWith(contentSuffix))
        .filter(e -> e.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false))
        .filter(e -> e.findAnnotation(ANNOTATION_CORE_MEDIA_TYPE).isPresent())
        .forEach(
            e -> {
              String prefix =
                  e.getName().substring(0, e.getName().length() - contentSuffix.length());
              if (!prefix.isEmpty()) {
                fieldNames.add(prefix);
              }
            });
    return new ArrayList<>(fieldNames);
  }

  /**
   * Checks if an element is a flattened content field from an inline Attachment type. For example,
   * "profilePicture_content" where "profilePicture" is of type Attachment. In the flattened model,
   * this is an element that ends with "_content", has the "_is_media_data" annotation, and has the
   * "Core.MediaType" annotation.
   *
   * @param entity The parent entity
   * @param element The element to check
   * @return <code>true</code> if the element is an inline attachment content field
   */
  public static boolean isInlineAttachmentContentField(
      CdsStructuredType entity, CdsElement element) {
    if (entity.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)) {
      return false; // This is a composition-based attachment entity, not inline
    }
    String elementName = element.getName();
    return elementName.contains("_")
        && element.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)
        && element.findAnnotation(ANNOTATION_CORE_MEDIA_TYPE).isPresent();
  }

  /**
   * Finds the inline attachment prefix for a given flattened element name. For example, given
   * "profilePicture_content", returns Optional of "profilePicture". Uses the known inline prefixes
   * from the entity to match against the element name.
   *
   * @param entity The parent entity
   * @param elementName The flattened element name
   * @return Optional containing the prefix, or empty if not an inline attachment field
   */
  public static Optional<String> getInlineAttachmentPrefix(
      CdsStructuredType entity, String elementName) {
    return getInlineAttachmentFieldNames(entity).stream()
        .filter(prefix -> elementName.startsWith(prefix + "_"))
        .findFirst();
  }

  /**
   * Extracts key fields from CdsData based on the entity definition.
   *
   * @param data The CdsData to extract keys from
   * @param entity The entity definition
   * @return A map of key fields and their values
   */
  public static Map<String, Object> extractKeys(CdsData data, CdsEntity entity) {
    Map<String, Object> keys = new HashMap<>();
    entity
        .keyElements()
        .forEach(
            keyElement -> {
              String keyName = keyElement.getName();
              Object value = data.get(keyName);
              if (value != null) {
                keys.put(keyName, value);
              }
            });
    return keys;
  }

  /**
   * Condenses the attachments from the given data into a list of {@link Attachments attachments}.
   * Supports both composition-based and inline attachment type fields.
   *
   * @param data the list of {@link CdsData} to process
   * @param entity the {@link CdsEntity entity} type of the given data
   * @return a list of {@link Attachments attachments} condensed from the data
   */
  public static List<Attachments> condenseAttachments(
      List<? extends CdsData> data, CdsEntity entity) {
    List<Attachments> resultList = new ArrayList<>();

    Validator validator =
        (path, element, value) -> {
          // For composition-based: path.target() is the attachment entity
          if (path.target().type().getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false)) {
            resultList.add(Attachments.of(path.target().values()));
          } else {
            // For inline type: extract prefixed fields from parent entity
            Optional<String> prefix =
                getInlineAttachmentPrefix(path.target().type(), element.getName());
            if (prefix.isPresent()) {
              Attachments attachment =
                  extractInlineAttachment(path.target().values(), prefix.get());
              // Avoid duplicates (same prefix already processed)
              if (resultList.stream()
                  .noneMatch(
                      existing ->
                          nonNull(existing.getContentId())
                              && existing.getContentId().equals(attachment.getContentId()))) {
                resultList.add(attachment);
              }
            }
          }
        };

    CdsDataProcessor.create().addValidator(MEDIA_CONTENT_FILTER, validator).process(data, entity);
    return resultList;
  }

  /**
   * Extracts inline attachment data from a parent entity's values by stripping the prefix. For
   * example, from "profilePicture_contentId" extracts "contentId".
   *
   * @param parentValues the parent entity values map
   * @param prefix the inline field prefix (e.g. "profilePicture")
   * @return an Attachments object with the extracted values
   */
  public static Attachments extractInlineAttachment(
      Map<String, Object> parentValues, String prefix) {
    Attachments attachment = Attachments.create();
    String prefixWithUnderscore = prefix + "_";
    parentValues.forEach(
        (key, value) -> {
          if (key.startsWith(prefixWithUnderscore)) {
            String logicalName = key.substring(prefixWithUnderscore.length());
            attachment.put(logicalName, value);
          }
        });
    return attachment;
  }

  public static boolean areKeysInData(Map<String, Object> keys, CdsData data) {
    return keys.entrySet().stream()
        .allMatch(
            entry -> {
              Object keyInData = data.get(entry.getKey());
              return nonNull(keyInData) && keyInData.equals(entry.getValue());
            });
  }

  /**
   * Removes the draft key "IsActiveEntity" from the given map of keys.
   *
   * @param keys The map of keys
   * @return A new map without the draft key
   */
  public static Map<String, Object> removeDraftKey(Map<String, Object> keys) {
    Map<String, Object> keyMap = new HashMap<>(keys);
    keyMap.entrySet().removeIf(entry -> entry.getKey().equals(Drafts.IS_ACTIVE_ENTITY));
    return keyMap;
  }

  private ApplicationHandlerHelper() {
    // avoid instantiation
  }
}

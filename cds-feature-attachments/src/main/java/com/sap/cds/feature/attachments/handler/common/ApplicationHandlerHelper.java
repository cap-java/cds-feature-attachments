/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.AttachmentValidationHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.draft.Drafts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The class {@link ApplicationHandlerHelper} provides helper methods for the
 * attachment application
 * handlers.
 */
public final class ApplicationHandlerHelper {
  private static final String ANNOTATION_IS_MEDIA_DATA = "_is_media_data";
  private static final String ANNOTATION_CORE_MEDIA_TYPE = "Core.MediaType";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Filter to support extraction of file name for attachment validation
   */
  public static final Filter FILE_NAME_FILTER = (path, element, type) -> element.getName().contentEquals("fileName");

  /**
   * A filter for media content fields. The filter checks if the entity is a media
   * entity and if the
   * element has the annotation "Core.MediaType".
   */
  public static final Filter MEDIA_CONTENT_FILTER = (path, element, type) -> isMediaEntity(path.target().type())
      && element.findAnnotation(ANNOTATION_CORE_MEDIA_TYPE).isPresent();

  /**
   * Checks if the data contains a content field.
   *
   * @param entity The {@link CdsEntity entity} type of the given the data to
   *               check
   * @param data   The data to check
   * @return <code>true</code> if the data contains a content field,
   *         <code>false</code> otherwise
   */
  public static boolean containsContentField(CdsEntity entity, List<? extends CdsData> data) {
    AtomicBoolean isIncluded = new AtomicBoolean();
    CdsDataProcessor.create()
        .addValidator(MEDIA_CONTENT_FILTER, (path, element, value) -> isIncluded.set(true))
        .process(data, entity);
    return isIncluded.get();
  }

  /**
   * Checks if the entity is a media entity. A media entity is an entity that is
   * annotated with the
   * annotation "_is_media_data".
   *
   * @param baseEntity The entity to check
   * @return <code>true</code> if the entity is a media entity, <code>false</code>
   *         otherwise
   */
  public static boolean isMediaEntity(CdsStructuredType baseEntity) {
    return baseEntity.getAnnotationValue(ANNOTATION_IS_MEDIA_DATA, false);
  }

  /**
   * Extracts key fields from CdsData based on the entity definition.
   *
   * @param data   The CdsData to extract keys from
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
   * Condenses the attachments from the given data into a list of
   * {@link Attachments attachments}.
   *
   * @param data   the list of {@link CdsData} to process
   * @param entity the {@link CdsEntity entity} type of the given data
   * @return a list of {@link Attachments attachments} condensed from the data
   */
  public static List<Attachments> condenseAttachments(
      List<? extends CdsData> data, CdsEntity entity) {
    List<Attachments> resultList = new ArrayList<>();

    Validator validator = (path, element, value) -> resultList.add(Attachments.of(path.target().values()));

    CdsDataProcessor.create().addValidator(MEDIA_CONTENT_FILTER, validator).process(data, entity);
    return resultList;
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

  public static void validateAcceptableMediaTypes(CdsEntity entity, List<CdsData> data) {
    List<String> allowedTypes = getEntityAcceptableMediaTypes(entity);
    String fileName = extractFileName(entity, data);
    AttachmentValidationHelper.validateMediaTypeForAttachment(fileName, allowedTypes);
  }

  private static List<String> getEntityAcceptableMediaTypes(CdsEntity entity) {
    Optional<CdsAnnotation<Object>> acceptableMediaTypeFromContent = entity.getElement("content")
        .findAnnotation("Core.AcceptableMediaTypes");

    return acceptableMediaTypeFromContent
        .map(a -> objectMapper.convertValue(
            a.getValue(),
            new TypeReference<List<String>>() {
            }))
        .orElse(List.of("*/*"));
  }

  private static String extractFileName(CdsEntity entity, List<? extends CdsData> data) {
    CdsDataProcessor processor = CdsDataProcessor.create();
    AtomicReference<String> fileNameRef = new AtomicReference<>();
    Validator validator = (path, element, value) -> {
      if (element.getName().contentEquals("fileName") && value instanceof String) {
        fileNameRef.set((String) value);
      }
    };

    processor.addValidator(FILE_NAME_FILTER, validator).process(data, entity);

    if (fileNameRef.get() == null) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Sam's custom test text: Filename is missing");
    }
    return fileNameRef.get();
  }

  private ApplicationHandlerHelper() {
    // avoid instantiation
  }
}

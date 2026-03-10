/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that attachments uploaded for a given entity conform to the allowed media types defined
 * via the {@code @Core.AcceptableMediaTypes} annotation.
 *
 * <p>Combines MIME type resolution, annotation lookup, and validation into a single utility.
 */
public final class MediaTypeValidator {

  private static final Logger logger = LoggerFactory.getLogger(MediaTypeValidator.class);

  static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";
  private static final List<String> WILDCARD_MEDIA_TYPE = List.of("*/*");
  private static final String CONTENT_ELEMENT = "content";
  private static final String ACCEPTABLE_MEDIA_TYPES_ANNOTATION = "Core.AcceptableMediaTypes";
  private static final TypeReference<List<String>> STRING_LIST_TYPE_REF = new TypeReference<>() {};
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static AssociationCascader cascader = new AssociationCascader();

  static void setCascader(AssociationCascader testCascader) {
    cascader = testCascader;
  }

  /**
   * Validates if the media type of the attachments in the given data are acceptable for the entity.
   *
   * @param entity the {@link CdsEntity entity} type of the given data
   * @param data the list of {@link CdsData} to process
   * @param cdsRuntime the CDS runtime
   * @throws ServiceException if the media type of any attachment is not acceptable
   */
  public static void validateMediaAttachments(
      CdsEntity entity, List<CdsData> data, CdsRuntime cdsRuntime) {
    if (entity == null) {
      return;
    }
    CdsModel cdsModel = cdsRuntime.getCdsModel();

    boolean areAttachmentsAvailable =
        ApplicationHandlerHelper.isMediaEntity(entity)
            || cascader.hasAttachmentPath(cdsModel, entity);

    if (!areAttachmentsAvailable) {
      return;
    }

    Map<String, List<String>> allowedTypesByElementName =
        getAcceptableMediaTypesFromEntity(entity, cdsModel);
    Map<String, Set<String>> fileNamesByElementName =
        AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, data);

    if (fileNamesByElementName == null || fileNamesByElementName.isEmpty()) {
      return;
    }

    validateAttachmentMediaTypes(fileNamesByElementName, allowedTypesByElementName);
  }

  // --------------- Annotation resolution ---------------

  /**
   * Resolves the acceptable media (MIME) types for the given {@link CdsEntity} by walking its
   * composition tree.
   */
  static Map<String, List<String>> getAcceptableMediaTypesFromEntity(
      CdsEntity entity, CdsModel model) {
    Map<String, List<String>> result = new HashMap<>();
    List<String> mediaEntityNames = cascader.findMediaEntityNames(model, entity);
    if (mediaEntityNames.isEmpty()) {
      return result;
    }
    for (String entityName : mediaEntityNames) {
      CdsEntity mediaEntity = model.getEntity(entityName);
      result.put(entityName, fetchAcceptableMediaTypes(mediaEntity));
    }
    return result;
  }

  private static List<String> fetchAcceptableMediaTypes(CdsEntity entity) {
    return getAcceptableMediaTypesAnnotation(entity)
        .map(CdsAnnotation::getValue)
        .map(value -> objectMapper.convertValue(value, STRING_LIST_TYPE_REF))
        .orElse(WILDCARD_MEDIA_TYPE);
  }

  static Optional<CdsAnnotation<Object>> getAcceptableMediaTypesAnnotation(CdsEntity entity) {
    return Optional.ofNullable(entity.getElement(CONTENT_ELEMENT))
        .flatMap(element -> element.findAnnotation(ACCEPTABLE_MEDIA_TYPES_ANNOTATION));
  }

  // --------------- MIME type resolution ---------------

  /**
   * Resolves the MIME type of a file based on its filename (specifically its extension).
   *
   * @param fileName the name of the file (including extension)
   * @return the resolved MIME type, or a default MIME type if it cannot be determined
   * @throws ServiceException if the filename is null or blank
   */
  static String resolveMimeType(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
    }

    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
      return fallbackToDefaultMimeType(fileName);
    }

    FileNameMap fileNameMap = URLConnection.getFileNameMap();
    String actualMimeType = fileNameMap.getContentTypeFor(fileName);

    if (actualMimeType == null) {
      return fallbackToDefaultMimeType(fileName);
    }
    return actualMimeType;
  }

  /**
   * Checks if a given MIME type is allowed based on a collection of acceptable media types.
   *
   * @param acceptableMediaTypes the collection of acceptable media types
   * @param mimeType the MIME type to check
   * @return true if the MIME type is allowed
   */
  static boolean isMimeTypeAllowed(Collection<String> acceptableMediaTypes, String mimeType) {
    if (mimeType == null) {
      return false;
    }

    if (acceptableMediaTypes == null
        || acceptableMediaTypes.isEmpty()
        || acceptableMediaTypes.contains("*/*")) return true;

    String baseMimeType = mimeType.trim().toLowerCase();
    Collection<String> normalizedTypes =
        acceptableMediaTypes.stream().map(type -> type.trim().toLowerCase()).toList();

    return normalizedTypes.stream()
        .anyMatch(
            type -> {
              return type.endsWith("/*")
                  ? baseMimeType.startsWith(type.substring(0, type.length() - 1))
                  : baseMimeType.equals(type);
            });
  }

  // --------------- Validation logic ---------------

  private static void validateAttachmentMediaTypes(
      Map<String, Set<String>> fileNamesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {

    boolean hasInvalidFiles =
        hasInvalidFilesByElementName(fileNamesByElementName, acceptableMediaTypesByElementName);

    if (hasInvalidFiles) {
      String allowedTypes =
          acceptableMediaTypesByElementName.values().stream()
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.joining(", "));
      throw new ServiceException(
          ErrorStatuses.UNSUPPORTED_MEDIA_TYPE,
          "Unsupported media type. Allowed types: " + allowedTypes);
    }
  }

  private static boolean hasInvalidFilesByElementName(
      Map<String, Set<String>> fileNamesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {
    return fileNamesByElementName.entrySet().stream()
        .anyMatch(
            entry -> {
              String elementName = entry.getKey();
              Set<String> files = entry.getValue();
              List<String> acceptableTypes =
                  acceptableMediaTypesByElementName.getOrDefault(elementName, WILDCARD_MEDIA_TYPE);
              return files.stream()
                  .anyMatch(
                      fileName -> {
                        String mimeType = resolveMimeType(fileName);
                        return !isMimeTypeAllowed(acceptableTypes, mimeType);
                      });
            });
  }

  private static String fallbackToDefaultMimeType(String fileName) {
    logger.warn(
        "Could not determine mime type for file: {}. Setting mime type to default: {}",
        fileName,
        DEFAULT_MEDIA_TYPE);
    return DEFAULT_MEDIA_TYPE;
  }

  private MediaTypeValidator() {
    // to prevent instantiation
  }
}

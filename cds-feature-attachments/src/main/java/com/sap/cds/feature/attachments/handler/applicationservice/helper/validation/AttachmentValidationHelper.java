/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.validation;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.AttachmentDataExtractor;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.media.MediaTypeResolver;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.media.MediaTypeService;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AttachmentValidationHelper {
  public static final List<String> WILDCARD_MEDIA_TYPE = List.of("*/*");

  /**
   * Validates if the media type of the attachment in the given fileName is acceptable
   *
   * @param entity the {@link CdsEntity entity} type of the given data
   * @param data the list of {@link CdsData} to process
   * @throws ServiceException if the media type of the attachment is not acceptable
   */
  public static void validateMediaAttachments(
      CdsEntity entity, List<CdsData> data, CdsRuntime cdsRuntime) {
    if (entity == null) {
      return;
    }
    CdsModel cdsModel = cdsRuntime.getCdsModel();
    Optional<CdsEntity> optionalServiceEntity = cdsModel.findEntity(entity.getQualifiedName());

    if (optionalServiceEntity.isEmpty()) {
      return;
    }

    CdsEntity serviceEntity = optionalServiceEntity.get();
    boolean hasNoAttachmentCompositions =
        !ApplicationHandlerHelper.deepSearchForAttachments(serviceEntity);

    if (!ApplicationHandlerHelper.isMediaEntity(serviceEntity) && hasNoAttachmentCompositions) {
      return;
    }

    Map<String, List<String>> allowedTypesByElementName =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(serviceEntity);
    Map<String, Set<String>> fileNamesByElementName =
        AttachmentDataExtractor.extractFileNamesByElement(serviceEntity, data);
    validateAttachmentMediaTypes(fileNamesByElementName, allowedTypesByElementName);
  }

  public static void validateAttachmentMediaTypes(
      Map<String, Set<String>> fileNamesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {

    Map<String, List<String>> invalidFiles =
        findInvalidFiles(fileNamesByElementName, acceptableMediaTypesByElementName);

    assertNoInvalidFiles(invalidFiles, acceptableMediaTypesByElementName);
  }

  private static Map<String, List<String>> findInvalidFiles(
      Map<String, Set<String>> fileNamesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {
    if (fileNamesByElementName == null || fileNamesByElementName.isEmpty()) {
      return Map.of();
    }
    Map<String, List<String>> invalidFiles = new HashMap<>();
    fileNamesByElementName.forEach(
        (elementName, files) -> {
          List<String> acceptableTypes =
              acceptableMediaTypesByElementName.getOrDefault(elementName, WILDCARD_MEDIA_TYPE);

          List<String> invalid =
              files.stream().filter(file -> !isAttachmentTypeValid(file, acceptableTypes)).toList();

          if (!invalid.isEmpty()) {
            invalidFiles.put(elementName, invalid);
          }
        });

    return invalidFiles;
  }

  private static void assertNoInvalidFiles(
      Map<String, List<String>> invalidFiles,
      Map<String, List<String>> acceptableMediaTypesByElementName) {

    if (!invalidFiles.isEmpty()) {
      throw buildUnsupportedFileTypeMessage(acceptableMediaTypesByElementName, invalidFiles);
    }
  }

  private static ServiceException buildUnsupportedFileTypeMessage(
      Map<String, List<String>> acceptableMediaTypesByElementName,
      Map<String, List<String>> invalidFilesByElement) {
    String message =
        invalidFilesByElement.entrySet().stream()
            .map(
                entry -> {
                  String element = entry.getKey();
                  String files = String.join(", ", entry.getValue());
                  String allowed =
                      String.join(
                          ", ",
                          acceptableMediaTypesByElementName.getOrDefault(
                              element, WILDCARD_MEDIA_TYPE));
                  return files + " (allowed: " + allowed + ") ";
                })
            .collect(Collectors.joining("; "));

    return new ServiceException(
        ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, "Unsupported file types detected: " + message);
  }

  private static boolean isAttachmentTypeValid(String fileName, List<String> acceptableTypes) {
    String mimeType = MediaTypeService.resolveMimeType(fileName);
    return MediaTypeService.isMimeTypeAllowed(acceptableTypes, mimeType);
  }

  private AttachmentValidationHelper() {
    // prevent instantiation
  }
}

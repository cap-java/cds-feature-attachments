/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AttachmentValidationHelper {
  private static AssociationCascader cascader = new AssociationCascader();

  static void setCascader(AssociationCascader testCascader) {
    cascader = testCascader;
  }

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

    List<String> mediaEntityNames =
        ApplicationHandlerHelper.isMediaEntity(entity)
            ? List.of(entity.getQualifiedName())
            : cascader.findMediaEntityNames(cdsModel, entity);

    if (mediaEntityNames.isEmpty()) {
      return;
    }

    // Resolve which media entities actually have the @Core.AcceptableMediaTypes annotation.
    // If none do, skip the entire validation – no data extraction, no MIME resolution needed.
    Map<String, List<String>> allowedTypesByElementName =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(cdsModel, mediaEntityNames);

    if (allowedTypesByElementName.isEmpty()) {
      return;
    }

    // validate the media types of the attachments
    Map<String, Set<String>> fileNamesByElementName =
        AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, data);
    validateAttachmentMediaTypes(fileNamesByElementName, allowedTypesByElementName);

    // validate the explicitly supplied mime types as well. The file name check alone can be
    // bypassed by pairing an allowed file name (e.g. "report.pdf") with a disallowed MIME type
    // (e.g. "text/html") that is persisted and served inline.
    Map<String, Set<String>> mimeTypesByElementName =
        AttachmentDataExtractor.extractMimeTypesByElement(entity, data);
    validateAttachmentMimeTypes(mimeTypesByElementName, allowedTypesByElementName);
  }

  private static void validateAttachmentMimeTypes(
      Map<String, Set<String>> mimeTypesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {

    Map<String, List<String>> invalidMimeTypes =
        findInvalidMimeTypesByElementName(
            mimeTypesByElementName, acceptableMediaTypesByElementName);

    if (!invalidMimeTypes.isEmpty()) {
      throw buildUnsupportedFileTypeMessage(acceptableMediaTypesByElementName, invalidMimeTypes);
    }
  }

  private static Map<String, List<String>> findInvalidMimeTypesByElementName(
      Map<String, Set<String>> mimeTypesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {
    if (mimeTypesByElementName == null || mimeTypesByElementName.isEmpty()) {
      return Map.of();
    }
    Map<String, List<String>> invalidMimeTypes = new HashMap<>();
    mimeTypesByElementName.forEach(
        (elementName, mimeTypes) -> {
          List<String> acceptableTypes = acceptableMediaTypesByElementName.get(elementName);
          if (acceptableTypes == null) {
            return;
          }

          List<String> invalid =
              mimeTypes.stream()
                  .filter(
                      mimeType -> !MediaTypeService.isMimeTypeAllowed(acceptableTypes, mimeType))
                  .toList();

          if (!invalid.isEmpty()) {
            invalidMimeTypes.put(elementName, invalid);
          }
        });

    return invalidMimeTypes;
  }

  private static void validateAttachmentMediaTypes(
      Map<String, Set<String>> fileNamesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {

    // Determine which uploaded files do not match the allowed media types
    Map<String, List<String>> invalidFiles =
        findInvalidFilesByElementName(fileNamesByElementName, acceptableMediaTypesByElementName);

    if (!invalidFiles.isEmpty()) {
      throw buildUnsupportedFileTypeMessage(acceptableMediaTypesByElementName, invalidFiles);
    }
  }

  private static Map<String, List<String>> findInvalidFilesByElementName(
      Map<String, Set<String>> fileNamesByElementName,
      Map<String, List<String>> acceptableMediaTypesByElementName) {
    // If no files are provided, there is nothing to validate → return empty result
    if (fileNamesByElementName == null || fileNamesByElementName.isEmpty()) {
      return Map.of();
    }
    // Will store, per element, the list of files that violate media type
    // constraints
    Map<String, List<String>> invalidFiles = new HashMap<>();
    fileNamesByElementName.forEach(
        (elementName, files) -> {
          // Only validate elements that have the @Core.AcceptableMediaTypes annotation.
          // Elements not in the map are unconstrained and can accept any media type.
          List<String> acceptableTypes = acceptableMediaTypesByElementName.get(elementName);
          if (acceptableTypes == null) {
            return;
          }

          // Filter out files whose media type is NOT allowed for this element
          List<String> invalid =
              files.stream()
                  .filter(
                      fileName -> {
                        String mimeType = MediaTypeService.resolveMimeType(fileName);
                        return !MediaTypeService.isMimeTypeAllowed(acceptableTypes, mimeType);
                      })
                  .toList();

          if (!invalid.isEmpty()) {
            invalidFiles.put(elementName, invalid);
          }
        });

    return invalidFiles;
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
                      String.join(", ", acceptableMediaTypesByElementName.get(element));
                  return files + " (allowed: " + allowed + ") ";
                })
            .collect(Collectors.joining("; "));

    return new ServiceException(
        ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, "Unsupported file types detected: " + message);
  }

  private AttachmentValidationHelper() {
    // to prevent instantiation
  }
}

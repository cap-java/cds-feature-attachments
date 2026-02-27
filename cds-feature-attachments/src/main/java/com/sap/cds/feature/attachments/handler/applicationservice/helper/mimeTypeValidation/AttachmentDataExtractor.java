/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AttachmentDataExtractor {
  private static final String FILE_NAME_FIELD = "fileName";
  public static final Filter FILE_NAME_FILTER =
      (path, element, type) -> element.getName().contentEquals(FILE_NAME_FIELD);

  /**
   * Extracts and validates file names of attachments from the given entity data.
   *
   * @param entity the CDS entity definition
   * @param data the incoming data containing attachment values
   * @return a map of element names to sets of associated file names
   */
  public static Map<String, Set<String>> extractAndValidateFileNamesByElement(
      CdsEntity entity, List<? extends CdsData> data) {
    // Collects file names from attachment-related elements in the entity
    Map<String, Set<String>> fileNamesByElementName = collectFileNamesByElementName(entity, data);
    // Ensures that all attachments have valid (non-null, non-empty) file names.
    ensureAttachmentsHaveFileNames(entity, data, fileNamesByElementName);
    return fileNamesByElementName;
  }

  private static Map<String, Set<String>> collectFileNamesByElementName(
      CdsEntity entity, List<? extends CdsData> data) {
    // Use CdsProcessor to traverse the data and collect file names for elements
    // named "fileName"
    Map<String, Set<String>> fileNamesByElementName = new HashMap<>();
    CdsDataProcessor processor = CdsDataProcessor.create();
    Validator fileNameValidator = generateFileNameFieldValidator(fileNamesByElementName);
    processor.addValidator(FILE_NAME_FILTER, fileNameValidator).process(data, entity);
    return fileNamesByElementName;
  }

  private static Validator generateFileNameFieldValidator(Map<String, Set<String>> result) {
    Validator validator =
        (path, element, value) -> {
          String fileName = requireString(value);
          String normalizedFileName = validateAndNormalize(fileName);
          String key = element.getDeclaringType().getQualifiedName();
          result.computeIfAbsent(key, k -> new HashSet<>()).add(normalizedFileName);
        };
    return validator;
  }

  private static String validateAndNormalize(String fileName) {
    String trimmedFileName = fileName.trim();
    if (trimmedFileName.isEmpty()) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename must not be blank");
    }

    int lastDotIndex = trimmedFileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == trimmedFileName.length() - 1) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Invalid filename format: " + fileName);
    }
    return trimmedFileName;
  }

  private static void ensureAttachmentsHaveFileNames(
      CdsEntity entity, List<? extends CdsData> data, Map<String, Set<String>> result) {
    // Collect attachment-related elements/fields from the entity
    List<CdsElement> attachmentElements =
        entity
            .elements()
            .filter(
                e -> {
                  // Only consider associations
                  if (!e.getType().isAssociation()) {
                    return false;
                  }
                  // Keep only associations targeting media entities
                  // that define acceptable media types
                  CdsAssociationType association = e.getType().as(CdsAssociationType.class);
                  return ApplicationHandlerHelper.isMediaEntity(association.getTarget())
                      && MediaTypeResolver.getAcceptableMediaTypesAnnotation(
                              association.getTarget())
                          .isPresent();
                })
            .toList();

    // Validate that required attachments have file names
    ensureFilenamesPresent(data, result, attachmentElements);
  }

  private static void ensureFilenamesPresent(
      List<? extends CdsData> data,
      Map<String, Set<String>> result,
      List<CdsElement> attachmentElements) {

    // Extract keys of fields that actually contain data
    Set<String> dataKeys = collectValidDataKeys(data);

    // Check if any required attachment is missing a filename
    boolean hasMissingFileName = hasMissingFileNames(result, attachmentElements, dataKeys);

    // If any attachment is missing a filename, throw and exception
    if (hasMissingFileName) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
    }
  }

  private static boolean hasMissingFileNames(
      Map<String, Set<String>> result,
      List<CdsElement> availableAttachmentElements,
      Set<String> dataKeys) {

    return availableAttachmentElements.stream()
        .filter(e -> dataKeys.contains(e.getName()))
        .anyMatch(
            element -> {
              CdsAssociationType assoc = element.getType().as(CdsAssociationType.class);
              String target = assoc.getTarget().getQualifiedName();
              Set<String> fileNames = result.get(target);
              return fileNames == null || fileNames.isEmpty();
            });
  }

  private static Set<String> collectValidDataKeys(List<? extends CdsData> data) {
    return data.stream()
        .flatMap(d -> d.entrySet().stream())
        .filter(entry -> !isEmpty(entry.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private static boolean isEmpty(Object value) {
    return value == null
        || (value instanceof String s && s.isBlank())
        || (value instanceof Collection<?> c && c.isEmpty())
        || (value instanceof Iterable<?> i && !i.iterator().hasNext());
  }

  private static String requireString(Object value) {
    if (value == null) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
    }
    if (!(value instanceof String s)) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename must be a string");
    }
    return s;
  }

  private AttachmentDataExtractor() {
    // Private constructor to prevent instantiation
  }
}

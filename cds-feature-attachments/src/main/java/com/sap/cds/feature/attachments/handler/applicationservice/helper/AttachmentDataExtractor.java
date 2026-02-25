/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.media.MediaTypeResolver;
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

  public static Map<String, Set<String>> extractFileNamesByElement(
      CdsEntity entity, List<? extends CdsData> data) {
    Map<String, Set<String>> fileNamesByElementName = collectFileNamesByElementName(entity, data);
    ensureAttachmentsHaveFileNames(entity, data, fileNamesByElementName);
    return fileNamesByElementName;
  }

  private static Map<String, Set<String>> collectFileNamesByElementName(
      CdsEntity entity, List<? extends CdsData> data) {
    Map<String, Set<String>> fileNamesByElementName = new HashMap<>();
    CdsDataProcessor processor = CdsDataProcessor.create();
    Validator fileNameValidator = createFileNameValidator(fileNamesByElementName);
    processor.addValidator(FILE_NAME_FILTER, fileNameValidator).process(data, entity);
    return fileNamesByElementName;
  }

  private static Validator createFileNameValidator(Map<String, Set<String>> result) {
    Validator validator =
        (path, element, value) -> {
          if (!(value instanceof String fileName)) {
            throw new ServiceException(
                ErrorStatuses.BAD_REQUEST,
                value == null ? "Filename is missing" : "Filename must be a string");
          }
          fileName = fileName.trim();
          if (fileName.isBlank()) {
            throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
          }
          String key = element.getDeclaringType().getQualifiedName();
          result.computeIfAbsent(key, k -> new HashSet<>()).add(fileName);
        };
    return validator;
  }

  private static void ensureAttachmentsHaveFileNames(
      CdsEntity entity, List<? extends CdsData> data, Map<String, Set<String>> result) {
    List<CdsElement> attachmentElements =
        entity
            .elements()
            .filter(
                e -> {
                  if (!e.getType().isAssociation()) {
                    return false;
                  }
                  CdsAssociationType association = e.getType().as(CdsAssociationType.class);
                  return association.isComposition()
                      && ApplicationHandlerHelper.isMediaEntity(association.getTarget())
                      && MediaTypeResolver.getAcceptableMediaTypesAnnotation(
                              association.getTarget())
                          .isPresent();
                })
            .toList();
    ensureFilenamesPresent(data, result, attachmentElements);
  }

  private static void ensureFilenamesPresent(
      List<? extends CdsData> data,
      Map<String, Set<String>> result,
      List<CdsElement> attachmentElements) {
    Set<String> dataKeys = collectValidDataKeys(data);
    List<CdsElement> availableAttachmentElements =
        filterAttachmentsPresentInData(attachmentElements, dataKeys);
    boolean hasMissingFileNames = hasMissingFileNames(result, availableAttachmentElements);
    if (hasMissingFileNames) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
    }
  }

  private static boolean hasMissingFileNames(
      Map<String, Set<String>> result, List<CdsElement> availableAttachmentElements) {

    return availableAttachmentElements.stream()
        .anyMatch(
            element -> {
              CdsAssociationType assoc = element.getType().as(CdsAssociationType.class);
              String target = assoc.getTarget().getQualifiedName();

              return !result.containsKey(target) || result.get(target).isEmpty();
            });
  }

  private static List<CdsElement> filterAttachmentsPresentInData(
      List<CdsElement> attachmentElements, Set<String> dataKeys) {
    return attachmentElements.stream().filter(e -> dataKeys.contains(e.getName())).toList();
  }

  private static Set<String> collectValidDataKeys(List<? extends CdsData> data) {
    return data.stream()
        .flatMap(d -> d.entrySet().stream())
        .filter(entry -> !isEmptyValue(entry.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private static boolean isEmptyValue(Object value) {
    return value == null
        || (value instanceof String s && s.isBlank())
        || (value instanceof Collection<?> c && c.isEmpty())
        || (value instanceof Iterable<?> i && !i.iterator().hasNext());
  }

  private AttachmentDataExtractor() {
    // Private constructor to prevent instantiation
  }
}

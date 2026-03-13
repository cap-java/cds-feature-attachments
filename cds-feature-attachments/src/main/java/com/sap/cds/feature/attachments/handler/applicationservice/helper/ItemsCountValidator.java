/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the number of items in composition associations against {@code @Validation.MaxItems}
 * and {@code @Validation.MinItems} annotations.
 *
 * <p>Annotation values can be:
 *
 * <ul>
 *   <li>An integer literal, e.g. {@code @Validation.MaxItems: 20}
 *   <li>A property name from the parent entity, e.g. {@code @Validation.MaxItems: 'maxCount'}
 * </ul>
 */
public final class ItemsCountValidator {

  private static final Logger logger = LoggerFactory.getLogger(ItemsCountValidator.class);

  public static final String ANNOTATION_MAX_ITEMS = "Validation.MaxItems";
  public static final String ANNOTATION_MIN_ITEMS = "Validation.MinItems";

  /**
   * Validates all composition associations of the given entity that have
   * {@code @Validation.MaxItems} or {@code @Validation.MinItems} annotations against the items
   * count in the provided data.
   *
   * @param entity the entity definition
   * @param data the request payload data
   * @param existingData the existing data from the database (for UPDATE); may be empty for CREATE
   * @return a list of validation violations found
   */
  public static List<ItemsCountViolation> validate(
      CdsEntity entity, List<? extends CdsData> data, List<? extends CdsData> existingData) {
    List<ItemsCountViolation> violations = new ArrayList<>();

    entity
        .elements()
        .filter(
            element ->
                element.getType().isAssociation()
                    && element.getType().as(CdsAssociationType.class).isComposition())
        .filter(
            element ->
                isMediaEntity(element)
                    && (hasAnnotation(element, ANNOTATION_MAX_ITEMS)
                        || hasAnnotation(element, ANNOTATION_MIN_ITEMS)))
        .forEach(
            element -> {
              String compositionName = element.getName();
              validateComposition(entity, element, compositionName, data, existingData, violations);
            });

    return violations;
  }

  private static boolean isMediaEntity(CdsElementDefinition element) {
    CdsEntity target = element.getType().as(CdsAssociationType.class).getTarget();
    return ApplicationHandlerHelper.isMediaEntity(target);
  }

  private static boolean hasAnnotation(CdsElementDefinition element, String annotationName) {
    return element.findAnnotation(annotationName).isPresent();
  }

  private static void validateComposition(
      CdsEntity entity,
      CdsElementDefinition element,
      String compositionName,
      List<? extends CdsData> dataList,
      List<? extends CdsData> existingDataList,
      List<ItemsCountViolation> violations) {

    for (int i = 0; i < dataList.size(); i++) {
      CdsData data = dataList.get(i);
      CdsData existingData =
          (existingDataList != null && i < existingDataList.size())
              ? existingDataList.get(i)
              : null;

      int itemCount = countItems(data, compositionName);
      if (itemCount < 0) {
        // composition not present in payload, skip validation
        continue;
      }

      Map<String, Object> parentData = mergeData(existingData, data);

      validateMaxItems(entity, element, compositionName, itemCount, parentData, violations);
      validateMinItems(entity, element, compositionName, itemCount, parentData, violations);
    }
  }

  /**
   * Counts items for a composition. If the composition is present in the request data, use that
   * count. Otherwise, return -1 to indicate the composition is not being modified.
   */
  private static int countItems(CdsData data, String compositionName) {
    Object compositionData = data.get(compositionName);
    if (compositionData == null) {
      // composition not in payload, not being modified
      return -1;
    }
    if (compositionData instanceof Collection<?> collection) {
      return collection.size();
    }
    return -1;
  }

  private static Map<String, Object> mergeData(CdsData existingData, CdsData requestData) {
    if (existingData == null) {
      return requestData;
    }
    CdsData merged = CdsData.create();
    merged.putAll(existingData);
    merged.putAll(requestData);
    return merged;
  }

  private static void validateMaxItems(
      CdsEntity entity,
      CdsElementDefinition element,
      String compositionName,
      int itemCount,
      Map<String, Object> parentData,
      List<ItemsCountViolation> violations) {
    Optional<Integer> maxItems = resolveAnnotationValue(element, ANNOTATION_MAX_ITEMS, parentData);
    if (maxItems.isPresent() && itemCount > maxItems.get()) {
      String entityName = getSimpleEntityName(entity);
      violations.add(
          new ItemsCountViolation(
              ItemsCountViolation.Type.MAX_ITEMS,
              compositionName,
              entityName,
              itemCount,
              maxItems.get()));
    }
  }

  private static void validateMinItems(
      CdsEntity entity,
      CdsElementDefinition element,
      String compositionName,
      int itemCount,
      Map<String, Object> parentData,
      List<ItemsCountViolation> violations) {
    Optional<Integer> minItems = resolveAnnotationValue(element, ANNOTATION_MIN_ITEMS, parentData);
    if (minItems.isPresent() && itemCount < minItems.get()) {
      String entityName = getSimpleEntityName(entity);
      violations.add(
          new ItemsCountViolation(
              ItemsCountViolation.Type.MIN_ITEMS,
              compositionName,
              entityName,
              itemCount,
              minItems.get()));
    }
  }

  /**
   * Resolves the annotation value to an integer. Supports:
   *
   * <ul>
   *   <li>Integer literal: {@code @Validation.MaxItems: 20} → 20
   *   <li>Property reference as string: {@code @Validation.MaxItems: 'maxCount'} → reads value of
   *       'maxCount' from parent entity data
   *   <li>Bare annotation (value = true): ignored, returns empty
   * </ul>
   */
  static Optional<Integer> resolveAnnotationValue(
      CdsElementDefinition element, String annotationName, Map<String, Object> parentData) {
    return element
        .findAnnotation(annotationName)
        .map(CdsAnnotation::getValue)
        .flatMap(value -> resolveValue(value, parentData, annotationName));
  }

  private static Optional<Integer> resolveValue(
      Object value, Map<String, Object> parentData, String annotationName) {
    if ("true".equals(value.toString())) {
      // bare annotation without value, ignore
      return Optional.empty();
    }

    // try direct integer
    if (value instanceof Number number) {
      return Optional.of(number.intValue());
    }

    String strValue = value.toString().trim();

    // try parsing as integer literal
    try {
      return Optional.of(Integer.parseInt(strValue));
    } catch (NumberFormatException e) {
      // not a plain integer, try as property reference
      logger.debug(
          "Annotation value '{}' is not an integer, trying as property reference", strValue);
    }

    // try as property reference from parent entity data
    if (parentData != null) {
      Object propertyValue = parentData.get(strValue);
      if (propertyValue instanceof Number number) {
        return Optional.of(number.intValue());
      }
      if (propertyValue != null) {
        try {
          return Optional.of(Integer.parseInt(propertyValue.toString()));
        } catch (NumberFormatException e) {
          logger.warn(
              "Cannot resolve {} annotation value '{}' to an integer from property value '{}'",
              annotationName,
              strValue,
              propertyValue);
        }
      } else {
        logger.debug(
            "Property '{}' referenced by {} annotation not found in entity data",
            strValue,
            annotationName);
      }
    }

    logger.warn("Cannot resolve {} annotation value '{}' to an integer", annotationName, strValue);
    return Optional.empty();
  }

  /**
   * Returns the simple (unqualified) entity name, e.g. "Incidents" from
   * "my.namespace.service.Incidents".
   */
  private static String getSimpleEntityName(CdsEntity entity) {
    String qualifiedName = entity.getQualifiedName();
    return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
  }

  private ItemsCountValidator() {
    // avoid instantiation
  }
}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates attachment counts against {@code @Validation.MaxItems} and {@code @Validation.MinItems}
 * annotations on composition elements.
 */
public class AttachmentCountValidator {

  private static final Logger logger = LoggerFactory.getLogger(AttachmentCountValidator.class);
  private static final String VALIDATION_MAX_ITEMS = "Validation.MaxItems";
  private static final String VALIDATION_MIN_ITEMS = "Validation.MinItems";

  public AttachmentCountValidator() {
    // No dependencies needed - services are passed to methods that need them
  }

  /**
   * Validates attachment counts for CREATE operations. Validates both MaxItems and MinItems since
   * non-draft entities are created directly in the active table and must be valid immediately.
   *
   * @param entity the target entity
   * @param data the request data containing attachments
   */
  public void validateForCreate(CdsEntity entity, List<CdsData> data) {
    logger.debug("Validating attachment counts for CREATE on entity {}", entity.getQualifiedName());
    validateFromRequestData(entity, data, true, true);
  }

  /**
   * Validates attachment counts for UPDATE operations. Validates both MaxItems and MinItems by
   * calculating final count from request data.
   *
   * @param entity the target entity
   * @param data the request data containing attachments
   */
  public void validateForUpdate(CdsEntity entity, List<CdsData> data) {
    logger.debug("Validating attachment counts for UPDATE on entity {}", entity.getQualifiedName());

    entity
        .elements()
        .filter(this::hasCountValidationAnnotation)
        .forEach(
            element -> {
              String compositionName = element.getName();

              // Check if this composition is being modified in the request
              boolean compositionInRequest =
                  data.stream().anyMatch(d -> d.containsKey(compositionName));

              if (compositionInRequest) {
                // Get the count from request data (this is the new state)
                int newCount = countAttachmentsInRequestData(data, compositionName);

                validateCount(element, newCount, compositionName);
              }
              // If composition not in request, it's not being modified - no validation needed
            });
  }

  /**
   * Validates attachment counts for DRAFT_SAVE operations. Reads parent entity with expanded
   * compositions and validates both MaxItems and MinItems per composition.
   *
   * @param entity the target entity
   * @param statement the CQN statement to query draft data
   * @param service the service to run the query (DraftService for draft operations)
   */
  public void validateForDraftSave(
      CdsEntity entity, CqnFilterableStatement statement, CqnService service) {
    logger.debug(
        "Validating attachment counts for DRAFT_SAVE on entity {}", entity.getQualifiedName());

    // Find all compositions with count validation annotations
    List<CdsElement> validatedCompositions =
        entity.elements().filter(this::hasCountValidationAnnotation).toList();

    if (validatedCompositions.isEmpty()) {
      logger.debug("No compositions with count validation annotations found");
      return;
    }

    logger.debug(
        "Found {} compositions with validation annotations: {}",
        validatedCompositions.size(),
        validatedCompositions.stream().map(CdsElement::getName).toList());

    // Build expand list for validated compositions only
    var expandColumns =
        validatedCompositions.stream().map(element -> CQL.to(element.getName()).expand()).toList();

    // Query parent entity with expanded compositions using the provided service
    // (DraftService for draft data, which knows about draft tables)
    Select<?> select = Select.from(statement.ref()).columns(expandColumns);
    statement.where().ifPresent(select::where);

    logger.debug("Running query via {}: {}", service.getClass().getSimpleName(), select);

    Result result = service.run(select);
    List<CdsData> parentEntities = result.listOf(CdsData.class);

    logger.debug("Query returned {} parent entities", parentEntities.size());
    for (CdsData parent : parentEntities) {
      logger.debug("Parent entity data keys: {}", parent.keySet());
      for (CdsElement element : validatedCompositions) {
        String compositionName = element.getName();
        Object compositionData = parent.get(compositionName);
        logger.debug(
            "Composition '{}' data type: {}, value: {}",
            compositionName,
            compositionData != null ? compositionData.getClass().getName() : "null",
            compositionData);
      }
    }

    // Validate each composition
    for (CdsElement element : validatedCompositions) {
      String compositionName = element.getName();
      int count = countAttachmentsFromParentData(parentEntities, compositionName);

      logger.debug("Draft save validation: {} has {} attachments", compositionName, count);
      validateCount(element, count, compositionName);
    }
  }

  private void validateFromRequestData(
      CdsEntity entity, List<CdsData> data, boolean checkMax, boolean checkMin) {
    entity
        .elements()
        .filter(this::hasCountValidationAnnotation)
        .forEach(
            element -> {
              String compositionName = element.getName();

              // Only validate if the composition is present in the request data
              boolean compositionInRequest =
                  data.stream().anyMatch(d -> d.containsKey(compositionName));

              if (compositionInRequest) {
                int count = countAttachmentsInRequestData(data, compositionName);

                if (checkMax) {
                  validateMaxItems(element, count, compositionName);
                }
                if (checkMin) {
                  validateMinItems(element, count, compositionName);
                }
              }
            });
  }

  private void validateCount(CdsElement element, int count, String compositionName) {
    validateMaxItems(element, count, compositionName);
    validateMinItems(element, count, compositionName);
  }

  private void validateMaxItems(CdsElement element, int count, String compositionName) {
    getMaxItemsValue(element)
        .ifPresent(
            maxItems -> {
              if (count > maxItems) {
                logger.debug(
                    "MaxItems validation failed: {} has {} items, max is {}",
                    compositionName,
                    count,
                    maxItems);
                throw new ServiceException(
                    ErrorStatuses.BAD_REQUEST,
                    "MaxItemsExceeded",
                    maxItems,
                    compositionName,
                    count);
              }
            });
  }

  private void validateMinItems(CdsElement element, int count, String compositionName) {
    getMinItemsValue(element)
        .ifPresent(
            minItems -> {
              if (count < minItems) {
                logger.debug(
                    "MinItems validation failed: {} has {} items, min is {}",
                    compositionName,
                    count,
                    minItems);
                throw new ServiceException(
                    ErrorStatuses.BAD_REQUEST,
                    "MinItemsNotReached",
                    minItems,
                    compositionName,
                    count);
              }
            });
  }

  private boolean hasCountValidationAnnotation(CdsElement element) {
    if (!element.getType().isAssociation()) {
      return false;
    }
    CdsAssociationType assocType = element.getType().as(CdsAssociationType.class);
    if (!assocType.isComposition()) {
      return false;
    }
    // Check if target is a media entity (attachment) and has either MaxItems or MinItems annotation
    boolean hasAnnotation =
        element.findAnnotation(VALIDATION_MAX_ITEMS).isPresent()
            || element.findAnnotation(VALIDATION_MIN_ITEMS).isPresent();

    return hasAnnotation && ApplicationHandlerHelper.isMediaEntity(assocType.getTarget());
  }

  private int countAttachmentsInRequestData(List<CdsData> data, String compositionName) {
    int count = 0;
    for (CdsData entry : data) {
      Object compositionData = entry.get(compositionName);
      if (compositionData instanceof List<?> attachments) {
        count += attachments.size();
      }
    }
    return count;
  }

  private int countAttachmentsFromParentData(List<CdsData> parentEntities, String compositionName) {
    int count = 0;
    for (CdsData parent : parentEntities) {
      Object compositionData = parent.get(compositionName);
      if (compositionData instanceof List<?> attachments) {
        count += attachments.size();
      }
    }
    return count;
  }

  private Optional<Integer> getMaxItemsValue(CdsElement element) {
    return element
        .findAnnotation(VALIDATION_MAX_ITEMS)
        .map(annotation -> ((Number) annotation.getValue()).intValue());
  }

  private Optional<Integer> getMinItemsValue(CdsElement element) {
    return element
        .findAnnotation(VALIDATION_MIN_ITEMS)
        .map(annotation -> ((Number) annotation.getValue()).intValue());
  }
}

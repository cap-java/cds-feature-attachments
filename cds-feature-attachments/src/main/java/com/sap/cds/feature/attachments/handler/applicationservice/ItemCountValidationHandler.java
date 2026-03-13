/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Message.Severity;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.utils.model.CqnUtils;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link ItemCountValidationHandler} validates that the number of items in annotated
 * compositions does not exceed {@code @Validation.MaxItems} or fall below
 * {@code @Validation.MinItems}. For active entity operations (ApplicationService CREATE/UPDATE),
 * violations produce errors that reject the request.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ItemCountValidationHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ItemCountValidationHandler.class);

  public static final String ANNOTATION_MAX_ITEMS = "Validation.MaxItems";
  public static final String ANNOTATION_MIN_ITEMS = "Validation.MinItems";
  public static final String MSG_KEY_MAX_ITEMS_EXCEEDED = "CompositionMaxItemsExceeded";
  public static final String MSG_KEY_MIN_ITEMS_NOT_MET = "CompositionMinItemsNotMet";

  private final AttachmentsReader attachmentsReader;

  public ItemCountValidationHandler(AttachmentsReader attachmentsReader) {
    this.attachmentsReader =
        requireNonNull(attachmentsReader, "attachmentsReader must not be null");
  }

  @Before(event = CqnService.EVENT_CREATE)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeCreate(CdsCreateEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    if (!hasItemCountAnnotations(target)) {
      return;
    }
    logger.debug("Validating item count on CREATE for entity {}", target.getQualifiedName());
    validateCompositionItemCounts(target, data, null, context.getMessages(), Severity.ERROR);
  }

  @Before(event = CqnService.EVENT_UPDATE)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeUpdate(CdsUpdateEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    if (!hasItemCountAnnotations(target)) {
      return;
    }
    logger.debug("Validating item count on UPDATE for entity {}", target.getQualifiedName());

    // Read existing data from DB to compute the effective count
    CqnSelect select = CqnUtils.toSelect(context.getCqn(), context.getTarget());
    List<? extends CdsData> existingData =
        attachmentsReader.readAttachments(context.getModel(), target, select);

    validateCompositionItemCounts(
        target, data, existingData, context.getMessages(), Severity.ERROR);
  }

  /**
   * Validates item counts on all annotated compositions of the given entity.
   *
   * @param entity the entity definition
   * @param requestData the request payload data
   * @param existingData existing data from DB (null for CREATE operations)
   * @param messages the Messages instance to add errors/warnings to
   * @param severity the severity to use (ERROR for active entities, WARNING for drafts)
   */
  public static void validateCompositionItemCounts(
      CdsEntity entity,
      List<? extends CdsData> requestData,
      List<? extends CdsData> existingData,
      Messages messages,
      Severity severity) {

    if (requestData == null || requestData.isEmpty()) {
      return;
    }

    entity
        .elements()
        .filter(ItemCountValidationHandler::isAnnotatedComposition)
        .forEach(
            element -> {
              String compositionName = element.getName();
              Optional<Integer> maxItems = getAnnotationIntValue(element, ANNOTATION_MAX_ITEMS);
              Optional<Integer> minItems = getAnnotationIntValue(element, ANNOTATION_MIN_ITEMS);

              if (maxItems.isEmpty() && minItems.isEmpty()) {
                return;
              }

              // Count items for each row in the request data
              for (CdsData row : requestData) {
                int itemCount = computeEffectiveItemCount(row, compositionName);

                if (itemCount < 0) {
                  // composition not present in payload, skip validation
                  continue;
                }

                String entitySimpleName = getSimpleName(entity.getQualifiedName());

                maxItems.ifPresent(
                    max -> {
                      if (itemCount > max) {
                        logger.debug(
                            "MaxItems violation: {} has {} items, max is {}",
                            compositionName,
                            itemCount,
                            max);
                        String msgKey =
                            resolveMessageKey(
                                MSG_KEY_MAX_ITEMS_EXCEEDED, entitySimpleName, compositionName);
                        addMessage(messages, severity, msgKey, max, compositionName);
                      }
                    });

                minItems.ifPresent(
                    min -> {
                      if (itemCount < min) {
                        logger.debug(
                            "MinItems violation: {} has {} items, min is {}",
                            compositionName,
                            itemCount,
                            min);
                        String msgKey =
                            resolveMessageKey(
                                MSG_KEY_MIN_ITEMS_NOT_MET, entitySimpleName, compositionName);
                        addMessage(messages, severity, msgKey, min, compositionName);
                      }
                    });
              }
            });
  }

  /**
   * Computes the effective item count for a composition. For CREATE: simply counts items in the
   * payload. For UPDATE: the payload list represents the new state (deep update semantics in CAP).
   *
   * <p>TODO: For future support of incremental updates (e.g., adding/removing individual items),
   * this method may need to accept existing data from DB and the entity definition to compute the
   * effective count by merging payload changes with existing data.
   *
   * @param requestRow a single row from the request payload
   * @param compositionName the name of the composition element
   * @return the effective item count, or -1 if the composition is not in the payload
   */
  private static int computeEffectiveItemCount(CdsData requestRow, String compositionName) {

    Object compositionValue = requestRow.get(compositionName);
    if (compositionValue == null) {
      // Composition not included in payload
      return -1;
    }

    if (compositionValue instanceof List<?> payloadItems) {
      // The payload for deep CREATE/UPDATE represents the full new state
      return payloadItems.size();
    }

    return -1;
  }

  /** Checks if an element is a composition with item count annotations. */
  public static boolean isAnnotatedComposition(CdsElement element) {
    if (!element.getType().isAssociation()) {
      return false;
    }
    CdsAssociationType assocType = element.getType().as(CdsAssociationType.class);
    if (!assocType.isComposition()) {
      return false;
    }
    return element.findAnnotation(ANNOTATION_MAX_ITEMS).isPresent()
        || element.findAnnotation(ANNOTATION_MIN_ITEMS).isPresent();
  }

  /**
   * Reads an integer annotation value from the element. Currently supports integer literals only.
   *
   * <p>TODO: Support property references (e.g., {@code @Validation.MaxItems: stock}) where the
   * value is read from the parent entity's property at runtime.
   *
   * <p>TODO: Support dynamic expressions (e.g., {@code @Validation.MaxItems: (stock > 20 ? 5 : 2)})
   * where the value is computed dynamically.
   *
   * @param element the CDS element definition
   * @param annotationName the annotation name
   * @return the integer value if present and parseable, empty otherwise
   */
  public static Optional<Integer> getAnnotationIntValue(CdsElement element, String annotationName) {
    return element
        .findAnnotation(annotationName)
        .map(CdsAnnotation::getValue)
        .flatMap(
            value -> {
              if (value instanceof Number number) {
                return Optional.of(number.intValue());
              }
              // TODO: handle property references (String value representing a property name)
              // TODO: handle dynamic expressions
              try {
                return Optional.of(Integer.parseInt(value.toString()));
              } catch (NumberFormatException e) {
                logger.warn(
                    "Annotation {} has non-integer value '{}', skipping validation",
                    annotationName,
                    value);
                return Optional.empty();
              }
            });
  }

  /** Checks whether any composition on this entity has item count annotations. */
  private static boolean hasItemCountAnnotations(CdsEntity entity) {
    return entity.elements().anyMatch(ItemCountValidationHandler::isAnnotatedComposition);
  }

  /**
   * Resolves the message key with i18n override support. Lookup order:
   *
   * <ol>
   *   <li>{baseKey}_{entitySimpleName}_{compositionName} (most specific)
   *   <li>{baseKey} (default fallback)
   * </ol>
   *
   * @param baseKey the base message key
   * @param entitySimpleName the simple entity name (without namespace)
   * @param compositionName the composition element name
   * @return the resolved message key
   */
  public static String resolveMessageKey(
      String baseKey, String entitySimpleName, String compositionName) {
    String specificKey = baseKey + "_" + entitySimpleName + "_" + compositionName;
    try {
      ResourceBundle bundle = ResourceBundle.getBundle("messages");
      if (bundle.containsKey(specificKey)) {
        return specificKey;
      }
    } catch (java.util.MissingResourceException e) {
      logger.trace("Message bundle not found, using default key: {}", baseKey);
    }
    return baseKey;
  }

  /**
   * Extracts the simple name from a fully qualified entity name.
   *
   * @param qualifiedName the fully qualified entity name (e.g., "my.namespace.Incidents")
   * @return the simple name (e.g., "Incidents")
   */
  private static String getSimpleName(String qualifiedName) {
    int lastDot = qualifiedName.lastIndexOf('.');
    return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
  }

  /**
   * Adds a message with the appropriate severity and target.
   *
   * @param messages the Messages instance
   * @param severity the message severity (ERROR or WARNING)
   * @param messageKey the i18n message key
   * @param limit the limit value (max or min)
   * @param compositionName the composition name for targeting
   */
  private static void addMessage(
      Messages messages, Severity severity, String messageKey, int limit, String compositionName) {
    switch (severity) {
      case ERROR -> messages.error(messageKey, limit, compositionName).target(compositionName);
      case WARNING -> messages.warn(messageKey, limit, compositionName).target(compositionName);
      default -> messages.info(messageKey, limit, compositionName).target(compositionName);
    }
  }
}

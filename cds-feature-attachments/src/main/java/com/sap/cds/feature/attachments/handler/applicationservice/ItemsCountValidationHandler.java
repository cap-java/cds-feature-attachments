/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link ItemsCountValidationHandler} is an event handler that validates the number of
 * items in compositions annotated with {@code @Validation.MaxItems} or
 * {@code @Validation.MinItems}.
 *
 * <p>During draft activation (draft save), violations are reported as warnings. During direct active
 * CREATE/UPDATE operations, violations are reported as errors.
 *
 * <p>The annotation values can be:
 *
 * <ul>
 *   <li>An integer literal, e.g. {@code @Validation.MaxItems: 20}
 *   <li>A property reference (string), e.g. {@code @Validation.MaxItems: 'stock'} — the property
 *       value is looked up from the entity data at runtime
 * </ul>
 *
 * <p>Error messages can be overridden per entity/property using the i18n key pattern:
 * {@code Validation_MaxItems/<EntityName>/<PropertyName>}. If no specific key is found, the base
 * key {@code Validation_MaxItems} is used.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ItemsCountValidationHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ItemsCountValidationHandler.class);

  static final String ANNOTATION_MAX_ITEMS = "Validation.MaxItems";
  static final String ANNOTATION_MIN_ITEMS = "Validation.MinItems";
  static final String MESSAGE_KEY_MAX_ITEMS = "Validation_MaxItems";
  static final String MESSAGE_KEY_MIN_ITEMS = "Validation_MinItems";

  private final ThreadDataStorageReader storageReader;

  public ItemsCountValidationHandler(ThreadDataStorageReader storageReader) {
    this.storageReader = requireNonNull(storageReader, "storageReader must not be null");
  }

  @Before(event = CqnService.EVENT_CREATE)
  @HandlerOrder(HandlerOrder.LATE)
  void validateOnCreate(CdsCreateEventContext context, List<CdsData> data) {
    validateItemsCount(context, context.getTarget(), data);
  }

  @Before(event = CqnService.EVENT_UPDATE)
  @HandlerOrder(HandlerOrder.LATE)
  void validateOnUpdate(CdsUpdateEventContext context, List<CdsData> data) {
    validateItemsCount(context, context.getTarget(), data);
  }

  void validateItemsCount(EventContext context, CdsEntity entity, List<CdsData> data) {
    boolean isDraftActivation = storageReader.get();
    Messages messages = context.getMessages();

    entity
        .elements()
        .filter(
            element ->
                element.getType().isAssociation()
                    && element.getType().as(CdsAssociationType.class).isComposition())
        .forEach(
            element -> {
              Optional<Object> maxItemsOpt =
                  element
                      .findAnnotation(ANNOTATION_MAX_ITEMS)
                      .map(CdsAnnotation::getValue)
                      .filter(v -> !"true".equals(v.toString()));
              Optional<Object> minItemsOpt =
                  element
                      .findAnnotation(ANNOTATION_MIN_ITEMS)
                      .map(CdsAnnotation::getValue)
                      .filter(v -> !"true".equals(v.toString()));

              if (maxItemsOpt.isEmpty() && minItemsOpt.isEmpty()) {
                return;
              }

              String compositionName = element.getName();

              for (CdsData d : data) {
                Object compositionData = d.get(compositionName);
                if (compositionData instanceof List<?> items) {
                  int count = items.size();

                  if (maxItemsOpt.isPresent()) {
                    int maxItems = resolveAnnotationValue(maxItemsOpt.get(), d);
                    if (maxItems >= 0 && count > maxItems) {
                      String messageKey =
                          resolveMessageKey(
                              MESSAGE_KEY_MAX_ITEMS, entity.getQualifiedName(), compositionName);
                      logger.debug(
                          "MaxItems violation on {}.{}: count={}, max={}",
                          entity.getQualifiedName(),
                          compositionName,
                          count,
                          maxItems);
                      Message message =
                          isDraftActivation
                              ? messages.warn(messageKey, compositionName, maxItems, count)
                              : messages.error(messageKey, compositionName, maxItems, count);
                      message.target("in", b -> b.to(compositionName));
                    }
                  }

                  if (minItemsOpt.isPresent()) {
                    int minItems = resolveAnnotationValue(minItemsOpt.get(), d);
                    if (minItems >= 0 && count < minItems) {
                      String messageKey =
                          resolveMessageKey(
                              MESSAGE_KEY_MIN_ITEMS, entity.getQualifiedName(), compositionName);
                      logger.debug(
                          "MinItems violation on {}.{}: count={}, min={}",
                          entity.getQualifiedName(),
                          compositionName,
                          count,
                          minItems);
                      Message message =
                          isDraftActivation
                              ? messages.warn(messageKey, compositionName, minItems, count)
                              : messages.error(messageKey, compositionName, minItems, count);
                      message.target("in", b -> b.to(compositionName));
                    }
                  }
                }
              }
            });

    if (!isDraftActivation) {
      messages.throwIfError();
    }
  }

  /**
   * Resolves the annotation value to an integer. Supports:
   *
   * <ul>
   *   <li>Integer/Number literals — used directly
   *   <li>String values — first tried as integer literal, then as a property reference in the entity
   *       data
   * </ul>
   *
   * @param annotationValue the raw annotation value
   * @param data the entity data for property reference resolution
   * @return the resolved integer value, or -1 if resolution failed
   */
  static int resolveAnnotationValue(Object annotationValue, CdsData data) {
    if (annotationValue instanceof Number number) {
      return number.intValue();
    }
    if (annotationValue instanceof String stringValue) {
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException e) {
        // Treat as property reference
        Object propertyValue = data.get(stringValue);
        if (propertyValue instanceof Number number) {
          return number.intValue();
        }
      }
    }
    return -1;
  }

  /**
   * Resolves the i18n message key following the Fiori elements approach. First tries a specific key
   * in the format {@code baseKey/entityName/propertyName}. If the specific key is defined in the
   * application's {@code messages.properties}, it is used. Otherwise, falls back to the base key.
   *
   * <p>Applications can override the error message for a specific entity/property by defining the
   * specific key in their {@code messages.properties}:
   *
   * <pre>
   * Validation_MaxItems/my.Entity/attachments = Custom message for {0}, max {1}, current {2}
   * </pre>
   *
   * @param baseKey the base message key (e.g. {@code Validation_MaxItems})
   * @param entityName the fully qualified entity name
   * @param propertyName the composition property name
   * @return the specific key if defined in the message bundle, otherwise the base key
   */
  static String resolveMessageKey(String baseKey, String entityName, String propertyName) {
    String specificKey = baseKey + "/" + entityName + "/" + propertyName;
    try {
      ResourceBundle bundle = ResourceBundle.getBundle("messages");
      bundle.getString(specificKey);
      return specificKey;
    } catch (MissingResourceException e) {
      return baseKey;
    }
  }
}

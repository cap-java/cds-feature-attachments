/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates {@code @Validation.MinItems} and {@code @Validation.MaxItems} constraints on attachment
 * compositions.
 *
 * <p>The annotation value may be an integer literal, a reference to a property on the parent entity
 * (resolved at runtime from the event data), or any other raw value whose {@code toString()}
 * produces a parseable integer.
 *
 * <p>During draft operations the validator emits a <em>warning</em> message. During active-entity
 * operations (create/update/save) it emits an <em>error</em> message and throws immediately via
 * {@link Messages#throwIfError()}.
 *
 * <p>Error messages follow the Fiori elements i18n-override pattern: the base message key is {@code
 * AttachmentMinItems} / {@code AttachmentMaxItems}. To override the message for a specific
 * composition property the key {@code AttachmentMinItems_<EntityName>_<PropertyName>} or {@code
 * AttachmentMaxItems_<EntityName>_<PropertyName>} is tried first. Since CDS resolves the
 * most-specific key it can find, only the specific key is emitted – a missing specific translation
 * naturally falls back to the generic one.
 */
public final class ItemCountValidator {

  static final String ANNOTATION_MIN_ITEMS = "Validation.MinItems";
  static final String ANNOTATION_MAX_ITEMS = "Validation.MaxItems";

  static final String MSG_MIN_ITEMS = "AttachmentMinItems";
  static final String MSG_MAX_ITEMS = "AttachmentMaxItems";

  private static final Logger logger = LoggerFactory.getLogger(ItemCountValidator.class);

  private ItemCountValidator() {}

  /**
   * Validates min/max items constraints for all attachment compositions on {@code entity} using the
   * payload {@code data} as source of truth for the attachment count.
   *
   * @param entity the root {@link CdsEntity} whose compositions are checked
   * @param data the request payload (used to count passed-in attachments)
   * @param eventContext the current {@link EventContext}
   * @param isDraft {@code true} when executing in draft context – issues warnings instead of errors
   */
  public static void validate(
      CdsEntity entity, List<CdsData> data, EventContext eventContext, boolean isDraft) {
    entity
        .compositions()
        .filter(ItemCountValidator::hasItemCountAnnotation)
        .forEach(comp -> validateComposition(comp, entity, data, eventContext, isDraft));
  }

  // ---------------------------------------------------------------------------
  // internals
  // ---------------------------------------------------------------------------

  /**
   * Returns {@code true} if the given composition element carries at least one item-count
   * constraint annotation ({@code @Validation.MinItems} or {@code @Validation.MaxItems}).
   *
   * <p>This method is {@code public} so it can be referenced from handler classes outside this
   * package (e.g. {@code DraftActiveAttachmentsHandler}).
   *
   * @param element the composition element to inspect
   * @return {@code true} if an item-count annotation is present
   */
  public static boolean hasItemCountAnnotation(CdsElement element) {
    return element.findAnnotation(ANNOTATION_MIN_ITEMS).isPresent()
        || element.findAnnotation(ANNOTATION_MAX_ITEMS).isPresent();
  }

  private static void validateComposition(
      CdsElement comp,
      CdsEntity parentEntity,
      List<CdsData> data,
      EventContext eventContext,
      boolean isDraft) {

    String compName = comp.getName();
    long attachmentCount = countInPayload(compName, data);

    Messages messages = eventContext.getMessages();

    // --- MinItems ---
    comp.findAnnotation(ANNOTATION_MIN_ITEMS)
        .flatMap(ann -> resolveIntValue(ann, parentEntity, data))
        .ifPresent(
            minItems -> {
              if (attachmentCount < minItems) {
                String msgKey = messageKey(MSG_MIN_ITEMS, parentEntity, compName);
                logger.debug(
                    "MinItems violation on {}.{}: count={}, min={}",
                    parentEntity.getQualifiedName(),
                    compName,
                    attachmentCount,
                    minItems);
                Message msg =
                    isDraft
                        ? messages.warn(msgKey, attachmentCount, minItems)
                        : messages.error(msgKey, attachmentCount, minItems);
                msg.target(compName);
              }
            });

    // --- MaxItems ---
    comp.findAnnotation(ANNOTATION_MAX_ITEMS)
        .flatMap(ann -> resolveIntValue(ann, parentEntity, data))
        .ifPresent(
            maxItems -> {
              if (attachmentCount > maxItems) {
                String msgKey = messageKey(MSG_MAX_ITEMS, parentEntity, compName);
                logger.debug(
                    "MaxItems violation on {}.{}: count={}, max={}",
                    parentEntity.getQualifiedName(),
                    compName,
                    attachmentCount,
                    maxItems);
                Message msg =
                    isDraft
                        ? messages.warn(msgKey, attachmentCount, maxItems)
                        : messages.error(msgKey, attachmentCount, maxItems);
                msg.target(compName);
              }
            });

    if (!isDraft) {
      messages.throwIfError();
    }
  }

  /** Counts the number of items in the named composition across all data entries. */
  @SuppressWarnings("unchecked")
  private static long countInPayload(String compName, List<CdsData> data) {
    long count = 0;
    for (CdsData entry : data) {
      Object compValue = entry.get(compName);
      if (compValue instanceof List<?> list) {
        count += list.size();
      }
    }
    return count;
  }

  /**
   * Resolves the annotation value to a long. Handles:
   *
   * <ul>
   *   <li>Integer literal (e.g. {@code 20})
   *   <li>Property reference – a string matching a numeric property name on the parent entity data
   *       (e.g. {@code stock})
   *   <li>Any other raw string whose {@code toString()} parses as a long
   * </ul>
   */
  static Optional<Long> resolveIntValue(
      CdsAnnotation<?> annotation, CdsEntity entity, List<CdsData> data) {
    Object raw = annotation.getValue();
    if (raw == null) {
      return Optional.empty();
    }

    // Direct numeric value
    if (raw instanceof Number number) {
      return Optional.of(number.longValue());
    }

    String strValue = raw.toString().trim();

    // Try parsing directly as a number first
    try {
      return Optional.of(Long.parseLong(strValue));
    } catch (NumberFormatException ignored) {
      // fall through
    }

    // Try resolving as a property reference on the first data item
    if (!data.isEmpty() && !strValue.isEmpty()) {
      Object propValue = data.get(0).get(strValue);
      if (propValue instanceof Number number) {
        return Optional.of(number.longValue());
      }
      if (propValue != null) {
        try {
          return Optional.of(Long.parseLong(propValue.toString().trim()));
        } catch (NumberFormatException e) {
          logger.warn(
              "Cannot resolve @{} value '{}' as integer from property '{}'",
              annotation.getName(),
              raw,
              strValue);
        }
      }
    }

    logger.warn(
        "Cannot resolve @{} annotation value '{}' to an integer – skipping validation",
        annotation.getName(),
        raw);
    return Optional.empty();
  }

  /**
   * Builds an i18n message key following the Fiori elements override pattern. The specific key
   * {@code <baseKey>_<SimpleName>_<propertyName>} is always used; apps without a specific
   * translation entry will receive the message text from the generic {@code <baseKey>} fallback.
   */
  static String messageKey(String baseKey, CdsEntity entity, String propertyName) {
    String simpleName = simpleEntityName(entity.getQualifiedName());
    return baseKey + "_" + simpleName + "_" + propertyName;
  }

  private static String simpleEntityName(String qualifiedName) {
    int dot = qualifiedName.lastIndexOf('.');
    return dot >= 0 ? qualifiedName.substring(dot + 1) : qualifiedName;
  }
}

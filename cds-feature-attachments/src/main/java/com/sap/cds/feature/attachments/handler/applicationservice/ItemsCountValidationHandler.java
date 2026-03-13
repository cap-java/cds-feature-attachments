/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ItemsCountValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ItemsCountViolation;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.utils.OrderConstants;
import com.sap.cds.services.utils.model.CqnUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler that validates the number of items in composition associations against
 * {@code @Validation.MaxItems} and {@code @Validation.MinItems} annotations.
 *
 * <p>During draft mode (draft activate), violations are reported as warnings, allowing the draft to
 * remain in an invalid state. On direct active entity operations (CREATE/UPDATE), violations are
 * reported as errors, rejecting the request.
 *
 * <p>Error messages can be overridden by applications using the Fiori elements i18n convention. The
 * base message keys are:
 *
 * <ul>
 *   <li>{@code AttachmentMaxItemsExceeded} for max items violations
 *   <li>{@code AttachmentMinItemsNotReached} for min items violations
 * </ul>
 *
 * <p>To override for a specific entity/property, define in your application's {@code
 * messages.properties}:
 *
 * <ul>
 *   <li>{@code AttachmentMaxItemsExceeded_<EntityName>_<CompositionName>}
 *   <li>{@code AttachmentMinItemsNotReached_<EntityName>_<CompositionName>}
 * </ul>
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ItemsCountValidationHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ItemsCountValidationHandler.class);

  private final AttachmentsReader attachmentsReader;
  private final ThreadDataStorageReader storageReader;

  public ItemsCountValidationHandler(
      AttachmentsReader attachmentsReader, ThreadDataStorageReader storageReader) {
    this.attachmentsReader =
        requireNonNull(attachmentsReader, "attachmentsReader must not be null");
    this.storageReader = requireNonNull(storageReader, "storageReader must not be null");
  }

  @Before
  @HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
  void validateOnCreate(CdsCreateEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    if (!hasCompositionsWithItemCountAnnotations(target)) {
      return;
    }

    logger.debug("Validating items count for CREATE event on entity {}", target.getQualifiedName());

    List<ItemsCountViolation> violations =
        ItemsCountValidator.validate(target, data, new ArrayList<>());

    if (!violations.isEmpty()) {
      boolean isDraft = Boolean.TRUE.equals(storageReader.get());
      reportViolations(violations, context.getMessages(), isDraft);
    }
  }

  @Before
  @HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
  void validateOnUpdate(CdsUpdateEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    if (!hasCompositionsWithItemCountAnnotations(target)) {
      return;
    }

    logger.debug("Validating items count for UPDATE event on entity {}", target.getQualifiedName());

    CqnSelect select = CqnUtils.toSelect(context.getCqn(), target);
    List<Attachments> existingData =
        attachmentsReader.readAttachments(context.getModel(), target, select);

    List<ItemsCountViolation> violations = ItemsCountValidator.validate(target, data, existingData);

    if (!violations.isEmpty()) {
      boolean isDraft = Boolean.TRUE.equals(storageReader.get());
      reportViolations(violations, context.getMessages(), isDraft);
    }
  }

  static boolean hasCompositionsWithItemCountAnnotations(CdsEntity entity) {
    return entity
        .elements()
        .filter(
            e ->
                e.getType().isAssociation()
                    && e.getType().as(CdsAssociationType.class).isComposition())
        .anyMatch(
            e ->
                ApplicationHandlerHelper.isMediaEntity(
                        e.getType().as(CdsAssociationType.class).getTarget())
                    && (e.findAnnotation(ItemsCountValidator.ANNOTATION_MAX_ITEMS).isPresent()
                        || e.findAnnotation(ItemsCountValidator.ANNOTATION_MIN_ITEMS).isPresent()));
  }

  /**
   * Reports validation violations as messages on the event context.
   *
   * <p>For active entity operations, violations are reported as errors via {@link
   * ServiceException}, causing the request to fail. For draft operations, violations are reported
   * as warnings via the {@link Messages} API, allowing the draft to be saved in an invalid state.
   *
   * <p>The message key follows the Fiori elements i18n override convention: {@code
   * <BaseKey>_<EntityName>_<CompositionName>}. Applications can override messages by defining the
   * specific key in their own {@code messages.properties}. The base keys are always provided as
   * fallback.
   *
   * <p>Message parameters:
   *
   * <ul>
   *   <li>{0} = the configured limit
   *   <li>{1} = the actual number of items
   * </ul>
   */
  static void reportViolations(
      List<ItemsCountViolation> violations, Messages messages, boolean isDraft) {
    for (ItemsCountViolation violation : violations) {
      String messageKey = violation.getBaseMessageKey();
      String target = violation.compositionName();

      if (isDraft) {
        messages.warn(messageKey, violation.limit(), violation.actualCount()).target(target);
      } else {
        throw new ServiceException(
            ErrorStatuses.CONFLICT, messageKey, violation.limit(), violation.actualCount());
      }
    }
  }
}

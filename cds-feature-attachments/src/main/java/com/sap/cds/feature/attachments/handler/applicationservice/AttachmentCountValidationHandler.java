/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.AttachmentCountValidator;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler that validates attachment counts against {@code @Validation.MaxItems} and
 * {@code @Validation.MinItems} annotations for ApplicationService events (CREATE, UPDATE).
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentCountValidationHandler implements EventHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(AttachmentCountValidationHandler.class);

  private final AttachmentCountValidator validator;

  public AttachmentCountValidationHandler(AttachmentCountValidator validator) {
    this.validator = requireNonNull(validator, "validator must not be null");
  }

  /**
   * Validates attachment counts before CREATE. Only validates MaxItems since MinItems doesn't apply
   * to new entities.
   *
   * @param context the create event context
   * @param data the request data
   */
  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void validateOnCreate(CdsCreateEventContext context, List<CdsData> data) {
    logger.debug(
        "Validating attachment counts for CREATE on entity {}",
        context.getTarget().getQualifiedName());
    validator.validateForCreate(context.getTarget(), data);
  }

  /**
   * Validates attachment counts before UPDATE. Validates both MaxItems and MinItems.
   *
   * @param context the update event context
   * @param data the request data
   */
  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void validateOnUpdate(CdsUpdateEventContext context, List<CdsData> data) {
    logger.debug(
        "Validating attachment counts for UPDATE on entity {}",
        context.getTarget().getQualifiedName());
    validator.validateForUpdate(context.getTarget(), data);
  }
}

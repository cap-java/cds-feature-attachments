/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.handler.common.AttachmentCountValidator;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler that validates attachment counts against {@code @Validation.MaxItems} and
 * {@code @Validation.MinItems} annotations for DraftService events (DRAFT_SAVE).
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftAttachmentCountValidationHandler implements EventHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(DraftAttachmentCountValidationHandler.class);

  private final AttachmentCountValidator validator;

  public DraftAttachmentCountValidationHandler(AttachmentCountValidator validator) {
    this.validator = requireNonNull(validator, "validator must not be null");
  }

  /**
   * Validates attachment counts before DRAFT_SAVE (activation). Validates both MaxItems and
   * MinItems against the final draft state.
   *
   * @param context the draft save event context
   */
  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void validateOnDraftSave(DraftSaveEventContext context) {
    logger.debug(
        "Validating attachment counts for DRAFT_SAVE on entity {}",
        context.getTarget().getQualifiedName());
    // DraftSaveEventContext.getCqn() returns a CqnSelect directly
    // Pass the DraftService to query draft tables correctly
    validator.validateForDraftSave(context.getTarget(), context.getCqn(), context.getService());
  }
}

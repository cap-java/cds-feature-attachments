/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageSetter;
import com.sap.cds.feature.attachments.handler.common.AttachmentCountValidator;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceName(value = "*", type = DraftService.class)
public class DraftActiveAttachmentsHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(DraftActiveAttachmentsHandler.class);
  private final ThreadDataStorageSetter threadLocalSetter;
  private final AttachmentCountValidator validator;

  public DraftActiveAttachmentsHandler(
      ThreadDataStorageSetter threadLocalSetter, AttachmentCountValidator validator) {
    this.threadLocalSetter =
        requireNonNull(threadLocalSetter, "threadLocalSetter must not be null");
    this.validator = requireNonNull(validator, "validator must not be null");
  }

  @On
  void processDraftSave(DraftSaveEventContext context) {
    threadLocalSetter.set(true, context::proceed);
  }

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void validateOnDraftSave(DraftSaveEventContext context) {
    logger.debug(
        "Validating attachment counts for DRAFT_SAVE on entity {}",
        context.getTarget().getQualifiedName());
    validator.validateForDraftSave(context.getTarget(), context.getCqn(), context.getService());
  }
}

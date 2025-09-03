/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.service.handler;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link DefaultAttachmentsServiceHandler} is an event handler that is called when an
 * attachment is created, marked as deleted, restored or read.
 *
 * <p>As the documents and content is stored in the database with this handler the handler sets the
 * isInternalStored flag to true in the create-context. Without this flag the content would be
 * deleted in the database.
 */
@ServiceName(value = "*", type = AttachmentService.class)
public class DefaultAttachmentsServiceHandler implements EventHandler {

  private static final int DEFAULT_ON = 10 * HandlerOrder.AFTER + HandlerOrder.LATE;

  private static final Logger logger =
      LoggerFactory.getLogger(DefaultAttachmentsServiceHandler.class);

  private final EndTransactionMalwareScanProvider malwareScanProvider;

  public DefaultAttachmentsServiceHandler(EndTransactionMalwareScanProvider malwareScanProvider) {
    this.malwareScanProvider =
        Objects.requireNonNull(malwareScanProvider, "malwareScanProvider must not be null");
  }

  @On
  @HandlerOrder(DEFAULT_ON)
  void createAttachment(AttachmentCreateEventContext context) {
    logger.debug(
        "Default Attachment Service handler called for creating attachment for entity '{}'",
        context.getAttachmentEntity().getQualifiedName());
    String contentId = (String) context.getAttachmentIds().get(Attachments.ID);
    context.getData().setStatus(StatusCode.SCANNING);
    context.setIsInternalStored(true);
    context.setContentId(contentId);
    context.setCompleted();
  }

  /**
   * After the attachment is created, register a {@link ChangeSetListener} to perform a malware scan
   * at the end of the transaction.
   *
   * @param context the attachment creation event context
   */
  @After
  void afterCreateAttachment(AttachmentCreateEventContext context) {
    ChangeSetListener listener =
        malwareScanProvider.getChangeSetListener(
            context.getAttachmentEntity(), context.getContentId());
    context.getChangeSetContext().register(listener);
  }

  @On
  @HandlerOrder(DEFAULT_ON)
  void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) {
    logger.debug(
        "Default Attachment Service handler called for marking attachment as deleted with document id {}",
        context.getContentId());

    // nothing to do as data are stored in the database and handled by the database
    context.setCompleted();
  }

  @On
  @HandlerOrder(DEFAULT_ON)
  void restoreAttachment(AttachmentRestoreEventContext context) {
    logger.debug(
        "Default Attachment Service handler called for restoring attachment for timestamp {}",
        context.getRestoreTimestamp());

    // nothing to do as data are stored in the database and handled by the database
    context.setCompleted();
  }

  @On
  @HandlerOrder(DEFAULT_ON)
  void readAttachment(AttachmentReadEventContext context) {
    logger.debug(
        "Default Attachment Service handler called for reading attachment with document id {}",
        context.getContentId());

    // nothing to do as data are stored in the database and handled by the database
    context.setCompleted();
  }
}

package com.sap.cds.feature.attachments.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.handler.constants.HandlerConstants;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreDeletedEventContext;
import com.sap.cds.feature.attachments.utilities.LoggingMarker;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
	* The class {@link DefaultAttachmentsServiceHandler} is an event handler that
	* is called when an attachment is created, marked as deleted, restored or read.
	* <p>
	* As the documents and content is stored in the database with this handler the
	* handler sets the isInternalStored flag to true in the create-context.
	* Without this flag the content would be deleted in the database.
	*/
@ServiceName(value = "*", type = AttachmentService.class)
public class DefaultAttachmentsServiceHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAttachmentsServiceHandler.class);
	private static final Marker create_marker = LoggingMarker.ATTACHMENT_SERVICE_CREATE_HANDLER.getMarker();
	private static final Marker delete_marker = LoggingMarker.ATTACHMENT_SERVICE_DELETE_HANDLER.getMarker();
	private static final Marker restore_delete_marker = LoggingMarker.ATTACHMENT_SERVICE_RESTORE_DELETE_HANDLER.getMarker();
	private static final Marker read_marker = LoggingMarker.ATTACHMENT_SERVICE_READ_HANDLER.getMarker();

	private final EndTransactionMalwareScanProvider endTransactionMalwareScanProvider;

	public DefaultAttachmentsServiceHandler(EndTransactionMalwareScanProvider endTransactionMalwareScanProvider) {
		this.endTransactionMalwareScanProvider = endTransactionMalwareScanProvider;
	}

	@On(event = AttachmentService.EVENT_CREATE_ATTACHMENT)
	@HandlerOrder(HandlerConstants.DEFAULT_ON)
	public void createAttachment(AttachmentCreateEventContext context) {
		logger.info(create_marker, "Default Attachment Service handler called for creating attachment for entity name: {}", context.getAttachmentEntity()
																																																																																																																								.getQualifiedName());
		var documentId = (String) context.getAttachmentIds().get(Attachments.ID);
		context.getData().setStatusCode(StatusCode.UNSCANNED);
		var listener = endTransactionMalwareScanProvider.getChangeSetListener(context.getAttachmentEntity(), documentId);
		context.getChangeSetContext().register(listener);
		context.setIsInternalStored(true);
		context.setDocumentId(documentId);
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_MARK_AS_DELETED)
	@HandlerOrder(HandlerConstants.DEFAULT_ON)
	public void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) {
		logger.info(delete_marker, "marking attachment as deleted with document id: {}", context.getDocumentId());

		//nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_RESTORE_DELETED)
	@HandlerOrder(HandlerConstants.DEFAULT_ON)
	public void restoreDeleteAttachment(AttachmentRestoreDeletedEventContext context) {
		logger.info(restore_delete_marker, "Default Attachment Service handler called for restoring attachment for timestamp: {}", context.getRestoreTimestamp());

		//nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_READ_ATTACHMENT)
	@HandlerOrder(HandlerConstants.DEFAULT_ON)
	public void readAttachment(AttachmentReadEventContext context) {
		logger.info(read_marker, "Default Attachment Service handler called for reading attachment with document id: {}", context.getDocumentId());

		//nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

}

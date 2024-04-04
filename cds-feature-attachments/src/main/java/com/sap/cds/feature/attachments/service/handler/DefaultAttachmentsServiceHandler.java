package com.sap.cds.feature.attachments.service.handler;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreDeletedEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = AttachmentService.class)
public class DefaultAttachmentsServiceHandler implements EventHandler {

	private static final int DEFAULT_ON = 10 * HandlerOrder.AFTER + HandlerOrder.LATE;

	@On(event = AttachmentService.EVENT_CREATE_ATTACHMENT)
	@HandlerOrder(DEFAULT_ON)
	public void createAttachment(AttachmentCreateEventContext context) {
		//TODO Malware Scan and remove setting status here
		context.getData().setStatusCode(StatusCode.CLEAN);
		context.setIsInternalStored(true);
		context.setDocumentId((String) context.getAttachmentIds().get(Attachments.ID));
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_MARK_AS_DELETED)
	@HandlerOrder(DEFAULT_ON)
	public void deleteAttachment(AttachmentMarkAsDeletedEventContext context) {
		//nothing to do
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_RESTORE_DELETED)
	@HandlerOrder(DEFAULT_ON)
	public void restoreDeleteAttachment(AttachmentRestoreDeletedEventContext context) {
		//nothing to do
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_READ_ATTACHMENT)
	@HandlerOrder(DEFAULT_ON)
	public void readAttachment(AttachmentReadEventContext context) {
		//nothing to do
		context.setCompleted();
	}

}

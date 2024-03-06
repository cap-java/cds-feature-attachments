package com.sap.cds.feature.attachments.service.handler;

import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;
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
		//TODO Malware Scan
		context.setIsExternalCreated(false);
		context.setDocumentId((String) context.getAttachmentIds().get(Attachments.ID));
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_UPDATE_ATTACHMENT)
	@HandlerOrder(DEFAULT_ON)
	public void updateAttachment(AttachmentUpdateEventContext context) {
		context.setIsExternalCreated(false);
		context.setDocumentId((String) context.getAttachmentIds().get(Attachments.ID));
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_DELETE_ATTACHMENT)
	@HandlerOrder(DEFAULT_ON)
	public void deleteAttachment(AttachmentDeleteEventContext context) {
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

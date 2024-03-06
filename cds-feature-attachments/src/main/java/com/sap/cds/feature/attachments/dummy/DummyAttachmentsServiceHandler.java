package com.sap.cds.feature.attachments.dummy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = AttachmentService.class)
public class DummyAttachmentsServiceHandler implements EventHandler {

	private static final Map<String, byte[]> documents = new HashMap<>();

	@On(event = AttachmentService.EVENT_CREATE_ATTACHMENT)
	public void createAttachment(AttachmentCreateEventContext context) throws IOException {
		var documentId = UUID.randomUUID().toString();
		documents.put(documentId, context.getData().getContent().readAllBytes());
		context.setIsExternalCreated(true);
		context.setDocumentId(documentId);
	}

	@On(event = AttachmentService.EVENT_UPDATE_ATTACHMENT)
	public void updateAttachment(AttachmentUpdateEventContext context) throws IOException {
		documents.put(context.getDocumentId(), context.getData().getContent().readAllBytes());
		context.setIsExternalCreated(true);
	}

	@On(event = AttachmentService.EVENT_DELETE_ATTACHMENT)
	public void deleteAttachment(AttachmentDeleteEventContext context) {
		//nothing to do
	}

	@On(event = AttachmentService.EVENT_READ_ATTACHMENT)
	public void readAttachment(AttachmentReadEventContext context) {
		var stream = new ByteArrayInputStream(documents.get(context.getDocumentId()));
		context.getData().setContent(stream);
	}

}

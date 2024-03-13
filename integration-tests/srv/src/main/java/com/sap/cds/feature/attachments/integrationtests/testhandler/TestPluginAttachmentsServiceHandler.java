package com.sap.cds.feature.attachments.integrationtests.testhandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO remove if integration tests are in place, only for manual testing for now
@ServiceName(value = "*", type = AttachmentService.class)
public class TestPluginAttachmentsServiceHandler implements EventHandler {

	private static final Marker marker = MarkerFactory.getMarker("DUMMY_HANDLER");
	private static final Logger logger = LoggerFactory.getLogger(TestPluginAttachmentsServiceHandler.class);

	private static final Map<String, byte[]> documents = new HashMap<>();

	@On(event = AttachmentService.EVENT_CREATE_ATTACHMENT)
	public void createAttachment(AttachmentCreateEventContext context) throws IOException {
		logger.info(marker, "CREATE Attachment called in dummy handler");
		var documentId = UUID.randomUUID().toString();
		documents.put(documentId, context.getData().getContent().readAllBytes());
		context.setIsExternalCreated(true);
		context.setDocumentId(documentId);
		context.setCompleted();
		logger.info(marker, "CREATE Attachment created attachment with document id {}", documentId);
	}

	@On(event = AttachmentService.EVENT_UPDATE_ATTACHMENT)
	public void updateAttachment(AttachmentUpdateEventContext context) throws IOException {
		logger.info(marker, "UPDATE Attachment called in dummy handler for document id {}", context.getDocumentId());
		documents.put(context.getDocumentId(), context.getData().getContent().readAllBytes());
		context.setIsExternalCreated(true);
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_DELETE_ATTACHMENT)
	public void deleteAttachment(AttachmentDeleteEventContext context) {
		logger.info(marker, "DELETE Attachment called in dummy handler for document id {}", context.getDocumentId());
		documents.remove(context.getDocumentId());
		context.setCompleted();
	}

	@On(event = AttachmentService.EVENT_READ_ATTACHMENT)
	public void readAttachment(AttachmentReadEventContext context) {
		logger.info(marker, "READ Attachment called in dummy handler for document id {}", context.getDocumentId());
		var document = documents.get(context.getDocumentId());
		var stream = Objects.nonNull(document) ? new ByteArrayInputStream(document) : null;
		context.getData().setContent(stream);
		context.setCompleted();
	}

}

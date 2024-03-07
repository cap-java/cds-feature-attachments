package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;
import com.sap.cds.services.ServiceDelegator;

//TODO implement
//TODO add java doc
//TODO exception handling
//TODO i18n properties
public class DefaultAttachmentsService extends ServiceDelegator implements AttachmentService {

	public DefaultAttachmentsService() {
		super(AttachmentService.DEFAULT_NAME);
	}

	@Override
	public InputStream readAttachment(String documentId) {
		var readContext = AttachmentReadEventContext.create();
		readContext.setDocumentId(documentId);
		readContext.setData(MediaData.create());

		emit(readContext);

		return readContext.getData().getContent();
	}

	@Override
	public AttachmentModificationResult createAttachment(CreateAttachmentInput input) {
		var createContext = AttachmentCreateEventContext.create();
		createContext.setAttachmentIds(input.attachmentIds());
		createContext.setAttachmentEntityName(input.attachmentEntityName());
		var mediaData = MediaData.create();
		mediaData.setFileName(input.fileName());
		mediaData.setMimeType(input.mimeType());
		mediaData.setContent(input.content());
		createContext.setData(mediaData);

		emit(createContext);

		return new AttachmentModificationResult(Boolean.TRUE.equals(createContext.getIsExternalCreated()), createContext.getDocumentId());
	}

	@Override
	public AttachmentModificationResult updateAttachment(UpdateAttachmentInput input) {
		var updateContext = AttachmentUpdateEventContext.create();
		updateContext.setDocumentId(input.documentId());
		updateContext.setAttachmentIds(input.attachmentIds());
		updateContext.setAttachmentEntityName(input.attachmentEntityName());
		var mediaData = MediaData.create();
		mediaData.setFileName(input.fileName());
		mediaData.setMimeType(input.mimeType());
		mediaData.setContent(input.content());
		updateContext.setData(mediaData);

		emit(updateContext);

		return new AttachmentModificationResult(Boolean.TRUE.equals(updateContext.getIsExternalCreated()), updateContext.getDocumentId());
	}

	@Override
	public void deleteAttachment(String documentId) {
		var deleteContext = AttachmentDeleteEventContext.create();
		deleteContext.setDocumentId(documentId);

		emit(deleteContext);
	}

}

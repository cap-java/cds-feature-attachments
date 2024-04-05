package com.sap.cds.feature.attachments.service;

import java.io.InputStream;
import java.time.Instant;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreDeletedEventContext;
import com.sap.cds.services.ServiceDelegator;

//TODO add java doc
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

		return new AttachmentModificationResult(Boolean.TRUE.equals(createContext.getIsInternalStored()), createContext.getDocumentId(), createContext.getData()
																																																																																																																																					.getStatusCode());
	}

	@Override
	public void markAsDeleted(String documentId) {
		var deleteContext = AttachmentMarkAsDeletedEventContext.create();
		deleteContext.setDocumentId(documentId);

		emit(deleteContext);
	}

	@Override
	public void restoreDeleted(Instant restoreTimestamp) {
		var restoreContext = AttachmentRestoreDeletedEventContext.create();
		restoreContext.setRestoreTimestamp(restoreTimestamp);

		emit(restoreContext);
	}

}

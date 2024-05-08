/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service;

import java.io.InputStream;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.feature.attachments.utilities.LoggingMarker;
import com.sap.cds.services.ServiceDelegator;

/**
	* Default implementation of the {@link AttachmentService} interface.
	* The main	purpose of this class is to set data in the corresponding context and
	* to call the emit method for the attachment service.
	*/
public class DefaultAttachmentsService extends ServiceDelegator implements AttachmentService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAttachmentsService.class);
	private static final Marker create_marker = LoggingMarker.ATTACHMENT_SERVICE_CREATE_METHOD.getMarker();
	private static final Marker delete_marker = LoggingMarker.ATTACHMENT_SERVICE_DELETE_METHOD.getMarker();
	private static final Marker restore_marker = LoggingMarker.ATTACHMENT_SERVICE_RESTORE_METHOD.getMarker();
	private static final Marker read_marker = LoggingMarker.ATTACHMENT_SERVICE_READ_METHOD.getMarker();

	public DefaultAttachmentsService() {
		super(DEFAULT_NAME);
	}

	@Override
	public InputStream readAttachment(String contentId) {
		logger.info(read_marker, "Reading attachment with document id: {}", contentId);

		var readContext = AttachmentReadEventContext.create();
		readContext.setContentId(contentId);
		readContext.setData(MediaData.create());

		emit(readContext);

		return readContext.getData().getContent();
	}

	@Override
	public AttachmentModificationResult createAttachment(CreateAttachmentInput input) {
		logger.info(create_marker, "Creating attachment for entity name: {}", input.attachmentEntity().getQualifiedName());

		var createContext = AttachmentCreateEventContext.create();
		createContext.setAttachmentIds(input.attachmentIds());
		createContext.setAttachmentEntity(input.attachmentEntity());
		var mediaData = MediaData.create();
		mediaData.setFileName(input.fileName());
		mediaData.setMimeType(input.mimeType());
		mediaData.setContent(input.content());
		createContext.setData(mediaData);

		emit(createContext);

		return new AttachmentModificationResult(Boolean.TRUE.equals(createContext.getIsInternalStored()),
				createContext.getContentId(), createContext.getData().getStatus());
	}

	@Override
	public void markAttachmentAsDeleted(String contentId) {
		logger.info(delete_marker, "Marking attachment as deleted for document id: {}", contentId);

		var deleteContext = AttachmentMarkAsDeletedEventContext.create();
		deleteContext.setContentId(contentId);

		emit(deleteContext);
	}

	@Override
	public void restoreAttachment(Instant restoreTimestamp) {
		logger.info(restore_marker, "Restoring deleted attachment for timestamp: {}", restoreTimestamp);
		var restoreContext = AttachmentRestoreEventContext.create();
		restoreContext.setRestoreTimestamp(restoreTimestamp);

		emit(restoreContext);
	}

}

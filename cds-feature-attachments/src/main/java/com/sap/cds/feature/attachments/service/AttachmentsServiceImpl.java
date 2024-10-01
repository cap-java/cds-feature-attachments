/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service;

import java.io.InputStream;
import java.time.Instant;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.ServiceDelegator;

/**
	* Implementation of the {@link AttachmentService} interface.
	* The main	purpose of this class is to set data in the corresponding context and
	* to call the emit method for the AttachmentService.
	*/
public class AttachmentsServiceImpl extends ServiceDelegator implements AttachmentService {

	public AttachmentsServiceImpl() {
		super(DEFAULT_NAME);
	}

	@Override
	public InputStream readAttachment(String contentId) {

		var readContext = AttachmentReadEventContext.create();
		readContext.setContentId(contentId);
		readContext.setData(MediaData.create());

		emit(readContext);

		return readContext.getData().getContent();
	}

	@Override
	public AttachmentModificationResult createAttachment(CreateAttachmentInput input) {

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

		var deleteContext = AttachmentMarkAsDeletedEventContext.create();
		deleteContext.setContentId(contentId);

		emit(deleteContext);
	}

	@Override
	public void restoreAttachment(Instant restoreTimestamp) {

		var restoreContext = AttachmentRestoreEventContext.create();
		restoreContext.setRestoreTimestamp(restoreTimestamp);

		emit(restoreContext);
	}

}

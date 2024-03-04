package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
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
		return null;
	}

	@Override
	public AttachmentModificationResult createAttachment(CreateAttachmentInput input) {
		return new AttachmentModificationResult(false, input.attachmentId());
	}

	@Override
	public AttachmentModificationResult updateAttachment(UpdateAttachmentInput input) {
		return new AttachmentModificationResult(false, input.attachmentId());
	}

	@Override
	public void deleteAttachment(String documentId) {

	}

}

package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
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
		public InputStream readAttachment(AttachmentReadEventContext context) {
				return null;
		}

		@Override
		public AttachmentModificationResult createAttachment(AttachmentCreateEventContext context) {
				return new AttachmentModificationResult(false, context.getAttachmentId());
		}

		@Override
		public AttachmentModificationResult updateAttachment(AttachmentUpdateEventContext context) {
				return new AttachmentModificationResult(false, context.getAttachmentId());
		}

		@Override
		public void deleteAttachment(AttachmentDeleteEventContext context) {
				//implementation will follow
		}

}

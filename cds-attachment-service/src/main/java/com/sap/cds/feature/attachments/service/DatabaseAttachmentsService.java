package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
import com.sap.cds.services.ServiceDelegator;

public class DatabaseAttachmentsService extends ServiceDelegator implements AttachmentService {

		public DatabaseAttachmentsService() {
				super(AttachmentService.DEFAULT_NAME);
		}

		@Override
		public InputStream readAttachment(AttachmentReadEventContext context) throws AttachmentAccessException {
				return null;
		}

		@Override
		public AttachmentStorageResult storeAttachment(AttachmentStoreEventContext context) throws AttachmentAccessException {
				return new AttachmentStorageResult(false, context.getAttachmentId());
		}

		@Override
		public AttachmentStorageResult updateAttachment(AttachmentUpdateEventContext context) throws AttachmentAccessException {
				return new AttachmentStorageResult(false, context.getAttachmentId());
		}

		@Override
		public void deleteAttachment(AttachmentDeleteEventContext context) throws AttachmentAccessException {
				//implementation will follow
		}

}

package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.handler.Handler;

public class DummyAttachmentsService implements AttachmentService {

		@Override
		public InputStream readAttachment(AttachmentReadEventContext context) throws AttachmentAccessException {
				return null;
		}

		@Override
		public AttachmentStorageResult storeAttachment(AttachmentStoreEventContext context) throws AttachmentAccessException {
				return null;
		}

		@Override
		public void before(String[] events, String[] entities, int order, Handler handler) {

		}

		@Override
		public void on(String[] events, String[] entities, int order, Handler handler) {

		}

		@Override
		public void after(String[] events, String[] entities, int order, Handler handler) {

		}

		@Override
		public void emit(EventContext context) {

		}

		@Override
		public String getName() {
				return AttachmentService.DEFAULT_NAME;
		}

}

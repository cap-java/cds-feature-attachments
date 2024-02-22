package com.sap.cds.feature.attachments.handler;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.AttachmentService;

class AttachmentsHandlerTest {

		private AttachmentsHandler cut;
		private AttachmentService service;

		@BeforeEach
		void setup() {
				service = mock(AttachmentService.class);
				cut = new AttachmentsHandler(service);
		}

		@Test
		void simpleUpdateDoesNotCallAttachment() {

		}

}

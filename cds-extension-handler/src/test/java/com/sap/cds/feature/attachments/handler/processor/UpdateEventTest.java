package com.sap.cds.feature.attachments.handler.processor;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.AttachmentService;

class UpdateEventTest {

		private UpdateEvent cut;
		private AttachmentService attachmentService;

		@BeforeEach
		void setup() {
				attachmentService = mock(AttachmentService.class);
				cut = new UpdateEvent(attachmentService);
		}

		@Test
		void serviceCalledForUpdate() {
				fail("not implemented");
		}

}

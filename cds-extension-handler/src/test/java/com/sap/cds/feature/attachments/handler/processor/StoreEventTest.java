package com.sap.cds.feature.attachments.handler.processor;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;

class StoreEventTest {

		private StoreEvent cut;
		private AttachmentService attachmentService;
		private ArgumentCaptor<AttachmentStoreEventContext> contextArgumentCaptor;

		@BeforeEach
		void setup() {
				attachmentService = mock(AttachmentService.class);
				cut = new StoreEvent(attachmentService);

				contextArgumentCaptor = ArgumentCaptor.forClass(AttachmentStoreEventContext.class);
		}

		@Test
		void storageCalled() {
				fail("not implemented");
		}

		private AttachmentFieldNames getDefaultFieldNames() {
				return new AttachmentFieldNames("key", Optional.of("documentId"), Optional.of("mimeType"), Optional.of("fileName"));
		}

}

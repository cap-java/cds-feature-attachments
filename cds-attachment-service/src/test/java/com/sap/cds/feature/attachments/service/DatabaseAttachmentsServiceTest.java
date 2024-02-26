package com.sap.cds.feature.attachments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;

class DatabaseAttachmentsServiceTest {

		private DatabaseAttachmentsService cut;

		@BeforeEach
		void setup() {
				cut = new DatabaseAttachmentsService();
		}

		@Test
		void readAttachmentDoesNotThrow() throws AttachmentAccessException {
				var stream = cut.readAttachment(AttachmentReadEventContext.create());
				assertThat(stream).isNull();
		}

		@Test
		void storeAttachmentReturnsDocumentId() throws AttachmentAccessException {
				var input = AttachmentStoreEventContext.create();
				input.setAttachmentId("some id");
				var result = cut.storeAttachment(input);
				assertThat(result.documentId()).isNotEmpty();
		}

		@Test
		void storeAttachmentReturnsNotExternalStored() throws AttachmentAccessException {
				var input = AttachmentStoreEventContext.create();
				var result = cut.storeAttachment(input);
				assertThat(result.isExternalStored()).isFalse();
		}

		@Test
		void updateAttachmentReturnsDocumentId() throws AttachmentAccessException {
				var input = AttachmentUpdateEventContext.create();
				input.setAttachmentId("some id");
				var result = cut.updateAttachment(input);
				assertThat(result.documentId()).isNotEmpty();
		}

		@Test
		void updateAttachmentReturnsNotExternalStored() throws AttachmentAccessException {
				var input = AttachmentUpdateEventContext.create();
				var result = cut.updateAttachment(input);
				assertThat(result.isExternalStored()).isFalse();
		}

		@Test
		void deleteDoesNotThrow() {
				var input = AttachmentDeleteEventContext.create();
				assertDoesNotThrow(() -> cut.deleteAttachment(input));
		}

}

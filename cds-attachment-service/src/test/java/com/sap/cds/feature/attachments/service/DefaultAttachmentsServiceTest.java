package com.sap.cds.feature.attachments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.model.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;

class DefaultAttachmentsServiceTest {

		private DefaultAttachmentsService cut;

		@BeforeEach
		void setup() {
				cut = new DefaultAttachmentsService();
		}

		@Test
		void readAttachmentDoesNotThrow() {
				var stream = cut.readAttachment(AttachmentReadEventContext.create());
				assertThat(stream).isNull();
		}

		@Test
		void createAttachmentReturnsDocumentId() {
				var input = AttachmentCreateEventContext.create();
				input.setAttachmentId("some id");
				var result = cut.createAttachment(input);
				assertThat(result.documentId()).isNotEmpty();
		}

		@Test
		void createAttachmentReturnsNotExternalStored() {
				var input = AttachmentCreateEventContext.create();
				var result = cut.createAttachment(input);
				assertThat(result.isExternalStored()).isFalse();
		}

		@Test
		void updateAttachmentReturnsDocumentId() {
				var input = AttachmentUpdateEventContext.create();
				input.setAttachmentId("some id");
				var result = cut.updateAttachment(input);
				assertThat(result.documentId()).isNotEmpty();
		}

		@Test
		void updateAttachmentReturnsNotExternalStored() {
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

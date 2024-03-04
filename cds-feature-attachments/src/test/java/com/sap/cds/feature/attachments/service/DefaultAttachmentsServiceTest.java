package com.sap.cds.feature.attachments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;

class DefaultAttachmentsServiceTest {

	private DefaultAttachmentsService cut;

	@BeforeEach
	void setup() {
		cut = new DefaultAttachmentsService();
	}

	@Test
	void readAttachmentDoesNotThrow() {
		var stream = cut.readAttachment("");
		assertThat(stream).isNull();
	}

	@Test
	void createAttachmentReturnsDocumentId() {
		var input = new CreateAttachmentInput("", "", "", "", null);
		var result = cut.createAttachment(input);
		assertThat(result.documentId()).isNotEmpty();
	}

	@Test
	void createAttachmentReturnsNotExternalStored() {
		var input = new CreateAttachmentInput("", "", "", "", null);
		var result = cut.createAttachment(input);
		assertThat(result.isExternalStored()).isFalse();
	}

	@Test
	void updateAttachmentReturnsDocumentId() {
		var input = new UpdateAttachmentInput("", "", "", "", "", null);
		var result = cut.updateAttachment(input);
		assertThat(result.documentId()).isNotEmpty();
	}

	@Test
	void updateAttachmentReturnsNotExternalStored() {
		var input = new UpdateAttachmentInput("", "", "", "", "", null);
		var result = cut.updateAttachment(input);
		assertThat(result.isExternalStored()).isFalse();
	}

	@Test
	void deleteDoesNotThrow() {
		assertDoesNotThrow(() -> cut.deleteAttachment(""));
	}

}

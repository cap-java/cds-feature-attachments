package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachmentMarkAsDeletedEventContextTest {

	private AttachmentMarkAsDeletedEventContext cut;

	@BeforeEach
	void setup() {
		cut = AttachmentMarkAsDeletedEventContext.create();
	}

	@Test
	void fieldsCanBeSetAndRead() {
		cut.setDocumentId("document id");

		assertThat(cut.getDocumentId()).isEqualTo("document id");
	}

}

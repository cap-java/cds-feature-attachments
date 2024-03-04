package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.MediaData;

class AttachmentUpdateEventContextTest {

	private AttachmentUpdateEventContext cut;

	@BeforeEach
	void setup() {
		cut = AttachmentUpdateEventContext.create();
	}

	@Test
	void fieldsCanBeStoredAndRead() throws IOException {
		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
			cut.setDocumentId("some update documentID");
			cut.setAttachmentId("some update attachmentID");
			cut.setAttachmentEntityName("some update attachment entity name");
			var mediaData = MediaData.create();
			mediaData.setMimeType("mime type");
			mediaData.setFileName("file name");
			mediaData.setContent(testStream);

			cut.setData(mediaData);

			assertThat(cut.getDocumentId()).isEqualTo("some update documentID");
			assertThat(cut.getAttachmentId()).isEqualTo("some update attachmentID");
			assertThat(cut.getAttachmentEntityName()).isEqualTo("some update attachment entity name");
			var responseMediaData = cut.getData();
			assertThat(responseMediaData.getFileName()).isEqualTo("file name");
			assertThat(responseMediaData.getMimeType()).isEqualTo("mime type");
			assertThat(responseMediaData.getContent()).isEqualTo(testStream);
		}
	}

}

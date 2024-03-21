package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;

class AttachmentReadEventContextTest {
	private AttachmentReadEventContext cut;

	@BeforeEach
	void setup() {
		cut = AttachmentReadEventContext.create();
	}

	@Test
	void fieldsCanBeSetAndRead() throws IOException {
		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
			cut.setDocumentId("some read documentID");
			var mediaData = MediaData.create();
			mediaData.setMimeType("mime type");
			mediaData.setFileName("file name");
			mediaData.setContent(testStream);

			cut.setData(mediaData);

			assertThat(cut.getDocumentId()).isEqualTo("some read documentID");
			var responseMediaData = cut.getData();
			assertThat(responseMediaData.getFileName()).isEqualTo("file name");
			assertThat(responseMediaData.getMimeType()).isEqualTo("mime type");
			assertThat(responseMediaData.getContent()).isEqualTo(testStream);
		}
	}

}

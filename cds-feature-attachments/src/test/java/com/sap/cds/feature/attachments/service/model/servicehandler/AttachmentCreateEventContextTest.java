package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.reflect.CdsEntity;

class AttachmentCreateEventContextTest {

	private AttachmentCreateEventContext cut;

	@BeforeEach
	void setup() {
		cut = AttachmentCreateEventContext.create();
	}

	@Test
	void fieldsCanBeStoredAndRead() throws IOException {
		Map<String, Object> keys = Map.of("ID1", "some create attachmentID", "ID2", "second id");
		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
			cut.setContentId("some create contentID");
			cut.setAttachmentIds(keys);
			var entity = mock(CdsEntity.class);
			cut.setAttachmentEntity(entity);
			var mediaData = MediaData.create();
			mediaData.setMimeType("mime type");
			mediaData.setFileName("file name");
			mediaData.setContent(testStream);

			cut.setData(mediaData);

			assertThat(cut.getContentId()).isEqualTo("some create contentID");
			assertThat(cut.getAttachmentIds()).isEqualTo(keys);
			assertThat(cut.getAttachmentEntity()).isEqualTo(entity);
			var responseMediaData = cut.getData();
			assertThat(responseMediaData.getFileName()).isEqualTo("file name");
			assertThat(responseMediaData.getMimeType()).isEqualTo("mime type");
			assertThat(responseMediaData.getContent()).isEqualTo(testStream);
		}
	}

}

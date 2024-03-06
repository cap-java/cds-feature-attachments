package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Attachment;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;

class UpdateAttachmentEventTest extends ModifyAttachmentEventTestBase {

	private ArgumentCaptor<UpdateAttachmentInput> contextArgumentCaptor;

	@BeforeEach
	void setup() {
		super.setup();
		contextArgumentCaptor = ArgumentCaptor.forClass(UpdateAttachmentInput.class);
	}

	@Override
	ModifyAttachmentEvent defineCut() {
		return new UpdateAttachmentEvent(attachmentService);
	}

	@Test
	void storageCalledWithAllFieldsFilledFromPath() throws IOException {
		var existingData = CdsData.create();
		existingData.put("documentId", "some document id");

		var attachment = defineDataAndExecuteEvent(existingData);

		verify(attachmentService).updateAttachment(contextArgumentCaptor.capture());
		var updateInput = contextArgumentCaptor.getValue();
		assertThat(updateInput.documentId()).isNotEmpty().isEqualTo(existingData.get("documentId"));
		assertThat(updateInput.attachmentIds()).containsEntry("ID", attachment.getId());
		assertThat(updateInput.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(updateInput.mimeType()).isEqualTo(attachment.getMimeType());
		assertThat(updateInput.fileName()).isEqualTo(attachment.getFileName());
		assertThat(updateInput.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void storageCalledWithAllFieldsFilledFromExistingData() throws IOException {
		var attachment = Attachments.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));
		var existingData = CdsData.create();
		existingData.put(MediaData.FILE_NAME, "some file name");
		existingData.put(MediaData.MIME_TYPE, "some mime type");
		existingData.put(Attachments.DOCUMENT_ID, "some document id");

		cut.processEvent(path, null, attachment.getContent(), existingData, Map.of("ID", attachment.getId(), "up__ID", "test"));

		verify(attachmentService).updateAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.documentId()).isNotEmpty().isEqualTo(existingData.get(Attachment.DOCUMENT_ID));
		assertThat(resultValue.attachmentIds()).hasSize(2).containsEntry("ID", attachment.getId()).containsEntry("up__ID", "test");
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isNotEmpty().isEqualTo(existingData.get(MediaData.MIME_TYPE));
		assertThat(resultValue.fileName()).isNotEmpty().isEqualTo(existingData.get(MediaData.FILE_NAME));
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void newDocumentIdStoredInPath() {
		var attachment = Attachments.create();
		attachment.setId("testing");
		var attachmentServiceResult = new AttachmentModificationResult(false, "some document id");
		when(attachmentService.updateAttachment(any())).thenReturn(attachmentServiceResult);
		when(target.values()).thenReturn(attachment);

		cut.processEvent(path, null, attachment.getContent(), CdsData.create(), Map.of("ID", attachment.getId()));

		assertThat(attachment.getDocumentId()).isEqualTo(attachmentServiceResult.documentId());
	}

	private Attachments defineDataAndExecuteEvent(CdsData existingData) throws IOException {
		var attachment = Attachments.create();
		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setMimeType("mimeType");
			attachment.setFileName("file name");
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));


		cut.processEvent(path, null, attachment.getContent(), existingData, Map.of("ID", attachment.getId()));
		return attachment;
	}

}

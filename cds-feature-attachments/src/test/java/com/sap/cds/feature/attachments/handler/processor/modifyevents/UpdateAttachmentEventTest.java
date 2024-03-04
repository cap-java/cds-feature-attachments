package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Attachment;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
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
		var fieldNames = getDefaultFieldNames();
		var existingData = CdsData.create();
		existingData.put("documentId", "some document id");

		var attachment = defineDataAndExecuteEvent(fieldNames, existingData);

		verify(attachmentService).updateAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.documentId()).isNotEmpty().isEqualTo(existingData.get("documentId"));
		assertThat(resultValue.attachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isEqualTo(attachment.getMimeType());
		assertThat(resultValue.fileName()).isEqualTo(attachment.getFileName());
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void storageCalledWithAllFieldsFilledFromExistingData() throws IOException {
		var fieldNames = getDefaultFieldNames();
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

		cut.processEvent(path, null, fieldNames, attachment.getContent(), existingData, attachment.getId());

		verify(attachmentService).updateAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.documentId()).isNotEmpty().isEqualTo(existingData.get(Attachment.DOCUMENT_ID));
		assertThat(resultValue.attachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isNotEmpty().isEqualTo(existingData.get(MediaData.MIME_TYPE));
		assertThat(resultValue.fileName()).isNotEmpty().isEqualTo(existingData.get(MediaData.FILE_NAME));
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void newDocumentIdStoredInPath() {
		var fieldNames = getDefaultFieldNames();
		var attachment = Attachments.create();
		var attachmentServiceResult = new AttachmentModificationResult(false, "some document id");
		when(attachmentService.updateAttachment(any())).thenReturn(attachmentServiceResult);
		when(target.values()).thenReturn(attachment);

		cut.processEvent(path, null, fieldNames, attachment.getContent(), CdsData.create(), attachment.getId());

		assertThat(attachment.getDocumentId()).isEqualTo(attachmentServiceResult.documentId());
	}

	@Test
	void noFieldNamesDoNotFillContext() throws IOException {
		var fieldNames = new AttachmentFieldNames("key", Optional.empty(), Optional.empty(), Optional.empty(), "");
		var existingData = CdsData.create();
		existingData.put("documentId", "some document id");

		var attachment = defineDataAndExecuteEvent(fieldNames, existingData);

		verify(attachmentService).updateAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isNull();
		assertThat(resultValue.fileName()).isNull();
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
		assertThat(attachment.getDocumentId()).isNull();
	}

	private Attachments defineDataAndExecuteEvent(AttachmentFieldNames fieldNames, CdsData existingData) throws IOException {
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


		cut.processEvent(path, null, fieldNames, attachment.getContent(), existingData, attachment.getId());
		return attachment;
	}

}

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
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;

class CreateAttachmentEventTest extends ModifyAttachmentEventTestBase {

	private ArgumentCaptor<CreateAttachmentInput> contextArgumentCaptor;

	@BeforeEach
	void setup() {
		super.setup();
		contextArgumentCaptor = ArgumentCaptor.forClass(CreateAttachmentInput.class);
	}

	@Override
	ModifyAttachmentEvent defineCut() {
		return new CreateAttachmentEvent(attachmentService);
	}

	@Test
	void storageCalledWithAllFieldsFilledFromPath() throws IOException {
		var fieldNames = getDefaultFieldNames();
		var attachment = prepareAndExecuteEventWithData(fieldNames);

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isEqualTo(attachment.getMimeType());
		assertThat(resultValue.fileName()).isEqualTo(attachment.getFilename());
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void noFieldNamesDoNotFillContext() throws IOException {
		var fieldNames = new AttachmentFieldNames("key", Optional.empty(), Optional.empty(), Optional.empty(), "content");
		var attachment = prepareAndExecuteEventWithData(fieldNames);

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isNull();
		assertThat(resultValue.fileName()).isNull();
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
		assertThat(attachment.getDocumentId()).isNull();
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
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));
		var existingData = CdsData.create();
		existingData.put("filename", "some file name");
		existingData.put("mimeType", "some mime type");

		cut.processEvent(path, null, fieldNames, attachment.getContent(), existingData, attachment.getId());

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isEqualTo(existingData.get("mimeType"));
		assertThat(resultValue.fileName()).isEqualTo(existingData.get("filename"));
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void documentIdStoredInPath() {
		var fieldNames = getDefaultFieldNames();
		var attachment = Attachments.create();
		var attachmentServiceResult = new AttachmentModificationResult(false, "some document id");
		when(attachmentService.createAttachment(any())).thenReturn(attachmentServiceResult);
		when(target.values()).thenReturn(attachment);

		cut.processEvent(path, null, fieldNames, attachment.getContent(), CdsData.create(), attachment.getId());

		assertThat(attachment.getDocumentId()).isEqualTo(attachmentServiceResult.documentId());
	}

	private Attachments prepareAndExecuteEventWithData(AttachmentFieldNames fieldNames) throws IOException {
		var attachment = Attachments.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setMimeType("mimeType");
			attachment.setFilename("file name");
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));

		cut.processEvent(path, null, fieldNames, attachment.getContent(), CdsData.create(), attachment.getId());
		return attachment;
	}

}

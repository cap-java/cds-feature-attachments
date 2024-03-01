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
import com.sap.cds.feature.attachments.generation.cds4j.unit.test.Attachment;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.model.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;

class UpdateAttachmentEventTest extends ModifyAttachmentEventTestBase {

	private ArgumentCaptor<AttachmentUpdateEventContext> contextArgumentCaptor;

	@BeforeEach
	void setup() {
		super.setup();
		contextArgumentCaptor = ArgumentCaptor.forClass(AttachmentUpdateEventContext.class);
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
		assertThat(resultValue.getDocumentId()).isNotEmpty().isEqualTo(existingData.get("documentId"));
		assertThat(resultValue.getAttachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.getMimeType()).isEqualTo(attachment.getMimeType());
		assertThat(resultValue.getFileName()).isEqualTo(attachment.getFilename());
		assertThat(resultValue.getContent()).isEqualTo(attachment.getContent());
	}

	@Test
	void storageCalledWithAllFieldsFilledFromExistingData() throws IOException {
		var fieldNames = getDefaultFieldNames();
		var attachment = Attachment.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));
		var existingData = CdsData.create();
		existingData.put("filename", "some file name");
		existingData.put("mimeType", "some mime type");
		existingData.put("documentId", "some document id");

		cut.processEvent(path, null, fieldNames, attachment.getContent(), existingData, attachment.getId());

		verify(attachmentService).updateAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.getAttachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.getDocumentId()).isNotEmpty().isEqualTo(existingData.get("documentId"));
		assertThat(resultValue.getMimeType()).isNotEmpty().isEqualTo(existingData.get("mimeType"));
		assertThat(resultValue.getFileName()).isNotEmpty().isEqualTo(existingData.get("filename"));
		assertThat(resultValue.getContent()).isEqualTo(attachment.getContent());
	}

	@Test
	void newDocumentIdStoredInPath() {
		var fieldNames = getDefaultFieldNames();
		var attachment = Attachment.create();
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
		assertThat(resultValue.getAttachmentId()).isEqualTo(attachment.getId());
		assertThat(resultValue.getMimeType()).isNull();
		assertThat(resultValue.getFileName()).isNull();
		assertThat(resultValue.getContent()).isEqualTo(attachment.getContent());
		assertThat(attachment.getDocumentId()).isNull();
	}

	private Attachment defineDataAndExecuteEvent(AttachmentFieldNames fieldNames, CdsData existingData) throws IOException {
		var attachment = Attachment.create();
		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setMimeType("mimeType");
			attachment.setFilename("file name");
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));


		cut.processEvent(path, null, fieldNames, attachment.getContent(), existingData, attachment.getId());
		return attachment;
	}

}

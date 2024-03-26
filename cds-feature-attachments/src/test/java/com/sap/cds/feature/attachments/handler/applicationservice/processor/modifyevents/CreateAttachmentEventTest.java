package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

class CreateAttachmentEventTest {

	private static final String TEST_FULL_NAME = "test.full.Name";

	private CreateAttachmentEvent cut;

	private AttachmentService attachmentService;
	private Path path;
	private ResolvedSegment target;
	private CdsEntity entity;
	private ArgumentCaptor<CreateAttachmentInput> contextArgumentCaptor;

	@BeforeEach
	void setup() {
		attachmentService = mock(AttachmentService.class);
		cut = new CreateAttachmentEvent(attachmentService);

		contextArgumentCaptor = ArgumentCaptor.forClass(CreateAttachmentInput.class);
		path = mock(Path.class);
		target = mock(ResolvedSegment.class);
		entity = mock(CdsEntity.class);
		when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
		when(target.entity()).thenReturn(entity);
		when(path.target()).thenReturn(target);
	}

	@Test
	void storageCalledWithAllFieldsFilledFromPath() throws IOException {
		var attachment = prepareAndExecuteEventWithData();

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentIds()).containsEntry("ID", attachment.getId());
		assertThat(resultValue.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(resultValue.mimeType()).isEqualTo(attachment.getMimeType());
		assertThat(resultValue.fileName()).isEqualTo(attachment.getFileName());
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void storageCalledWithAllFieldsFilledFromExistingData() throws IOException {
		var attachment = Attachments.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setId(UUID.randomUUID().toString());
			attachment.put("up__ID", "test");
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));
		var existingData = CdsData.create();
		existingData.put(MediaData.FILE_NAME, "some file name");
		existingData.put(MediaData.MIME_TYPE, "some mime type");

		cut.processEvent(path, null, attachment.getContent(), existingData, Map.of("ID", attachment.getId(), "up__ID", "test"), mock(EventContext.class));

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var createInput = contextArgumentCaptor.getValue();
		assertThat(createInput.attachmentIds()).hasSize(2).containsEntry("ID", attachment.getId())
				.containsEntry("up__ID", "test");
		assertThat(createInput.attachmentEntityName()).isEqualTo(TEST_FULL_NAME);
		assertThat(createInput.mimeType()).isEqualTo(existingData.get(MediaData.MIME_TYPE));
		assertThat(createInput.fileName()).isEqualTo(existingData.get(MediaData.FILE_NAME));
		assertThat(createInput.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void documentIdStoredInPath() {
		var attachment = Attachments.create();
		attachment.setId("test");
		var attachmentServiceResult = new AttachmentModificationResult(false, "some document id");
		when(attachmentService.createAttachment(any())).thenReturn(attachmentServiceResult);
		when(target.values()).thenReturn(attachment);

		cut.processEvent(path, null, attachment.getContent(), CdsData.create(), Map.of("ID", attachment.getId()), mock(EventContext.class));

		assertThat(attachment.getDocumentId()).isEqualTo(attachmentServiceResult.documentId());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void contentIsReturnedIfNotExternalStored(boolean isExternalStored) throws IOException {
		var attachment = Attachments.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(isExternalStored, "id"));

		var result = cut.processEvent(path, null, attachment.getContent(), CdsData.create(), Map.of("ID", attachment.getId()), mock(EventContext.class));

		var expectedContent = isExternalStored ? attachment.getContent() : null;
		assertThat(result).isEqualTo(expectedContent);
	}

	private Attachments prepareAndExecuteEventWithData() throws IOException {
		var attachment = Attachments.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setMimeType("mimeType");
			attachment.setFileName("file name");
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id"));

		cut.processEvent(path, null, attachment.getContent(), CdsData.create(), Map.of("ID", attachment.getId()), mock(EventContext.class));
		return attachment;
	}

}

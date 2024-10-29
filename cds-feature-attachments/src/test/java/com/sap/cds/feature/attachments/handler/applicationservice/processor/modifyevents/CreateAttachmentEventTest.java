package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.runtime.CdsRuntime;

class CreateAttachmentEventTest {

	private static final String TEST_FULL_NAME = "test.full.Name";

	private CreateAttachmentEvent cut;

	private AttachmentService attachmentService;
	private ListenerProvider listenerProvider;
	private Path path;
	private ResolvedSegment target;
	private CdsEntity entity;
	private ArgumentCaptor<CreateAttachmentInput> contextArgumentCaptor;
	private EventContext eventContext;
	private ChangeSetContext changeSetContext;

	@BeforeEach
	void setup() {
		attachmentService = mock(AttachmentService.class);
		listenerProvider = mock(ListenerProvider.class);
		cut = new CreateAttachmentEvent(attachmentService, listenerProvider);

		contextArgumentCaptor = ArgumentCaptor.forClass(CreateAttachmentInput.class);
		path = mock(Path.class);
		target = mock(ResolvedSegment.class);
		entity = mock(CdsEntity.class);
		eventContext = mock(EventContext.class);
		changeSetContext = mock(ChangeSetContext.class);
		when(eventContext.getChangeSetContext()).thenReturn(changeSetContext);
		when(target.entity()).thenReturn(entity);
		when(path.target()).thenReturn(target);
	}

	@Test
	void storageCalledWithAllFieldsFilledFromPath() {
		when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
		var attachment = prepareAndExecuteEventWithData();

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentIds()).containsEntry("ID", attachment.getId());
		assertThat(resultValue.attachmentEntity()).isEqualTo(entity);
		assertThat(resultValue.mimeType()).isEqualTo(attachment.getMimeType());
		assertThat(resultValue.fileName()).isEqualTo(attachment.getFileName());
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void storageCalledWithAllFieldsFilledFromExistingData() {
		when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
		var attachment = Attachments.create();

		attachment.setContent(mock(InputStream.class));
		attachment.setId(UUID.randomUUID().toString());
		attachment.put("up__ID", "test");

		when(target.values()).thenReturn(attachment);
		when(target.keys()).thenReturn(Map.of("ID", attachment.getId(), "up__ID", "test"));
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id", "test"));
		var existingData = CdsData.create();
		existingData.put(MediaData.FILE_NAME, "some file name");
		existingData.put(MediaData.MIME_TYPE, "some mime type");

		cut.processEvent(path, attachment.getContent(), existingData, eventContext);

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var createInput = contextArgumentCaptor.getValue();
		assertThat(createInput.attachmentIds()).hasSize(2).containsEntry("ID", attachment.getId()).containsEntry("up__ID",
				"test");
		assertThat(createInput.attachmentEntity()).isEqualTo(entity);
		assertThat(createInput.mimeType()).isEqualTo(existingData.get(MediaData.MIME_TYPE));
		assertThat(createInput.fileName()).isEqualTo(existingData.get(MediaData.FILE_NAME));
		assertThat(createInput.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void resultFromServiceStoredInPath() {
		var attachment = Attachments.create();
		attachment.setId("test");
		var attachmentServiceResult = new AttachmentModificationResult(false, "some document id", "test");
		when(attachmentService.createAttachment(any())).thenReturn(attachmentServiceResult);
		when(target.values()).thenReturn(attachment);

		cut.processEvent(path, attachment.getContent(), CdsData.create(), eventContext);

		assertThat(attachment.getContentId()).isEqualTo(attachmentServiceResult.contentId());
		assertThat(attachment.getStatus()).isEqualTo(attachmentServiceResult.status());
	}

	@Test
	void changesetIstRegistered() {
		var contentId = "document id";
		var runtime = mock(CdsRuntime.class);
		when(eventContext.getCdsRuntime()).thenReturn(runtime);
		var listener = mock(ChangeSetListener.class);
		when(listenerProvider.provideListener(contentId, runtime)).thenReturn(listener);
		when(attachmentService.createAttachment(any())).thenReturn(
				new AttachmentModificationResult(false, contentId, "test"));

		cut.processEvent(path, null, CdsData.create(), eventContext);

		verify(changeSetContext).register(listener);
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
		when(attachmentService.createAttachment(any())).thenReturn(
				new AttachmentModificationResult(isExternalStored, "id", "test"));

		var result = cut.processEvent(path, attachment.getContent(), CdsData.create(), eventContext);

		var expectedContent = isExternalStored ? attachment.getContent() : null;
		assertThat(result).isEqualTo(expectedContent);
	}

	private Attachments prepareAndExecuteEventWithData() {
		var attachment = Attachments.create();

		attachment.setContent(mock(InputStream.class));
		attachment.setMimeType("mimeType");
		attachment.setFileName("file name");
		attachment.setId(UUID.randomUUID().toString());

		when(target.values()).thenReturn(attachment);
		when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
		when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "id", "test"));

		cut.processEvent(path, attachment.getContent(), CdsData.create(), eventContext);
		return attachment;
	}

}

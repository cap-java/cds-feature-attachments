package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.runtime.CdsRuntime;

class CreateAttachmentEventTest {

	private static final String TEST_FULL_NAME = "AdminService.Books.covers";
	private static final String PARENT_FULL_NAME = "AdminService.Books";
	private CreateAttachmentEvent cut;

	private AttachmentService attachmentService;
	private ListenerProvider listenerProvider;
	private Path path;
	private ResolvedSegment target;
	private CdsEntity attachmentEntity;
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
		attachmentEntity = mock(CdsEntity.class);
		when(attachmentEntity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
		eventContext = mock(EventContext.class);
		changeSetContext = mock(ChangeSetContext.class);
		when(eventContext.getChangeSetContext()).thenReturn(changeSetContext);
		when(target.entity()).thenReturn(attachmentEntity);
		when(path.target()).thenReturn(target);

		CdsAssociationType assocType = mock(CdsAssociationType.class);
		CdsElement assoc = mock(CdsElement.class);
		when(assoc.getName()).thenReturn("up_");
		when(assoc.getType()).thenReturn(assocType);
		when(assocType.getTarget()).thenReturn(attachmentEntity);
		when(assocType.refs()).thenReturn(Stream.of(mock(CqnElementRef.class)));
		when(attachmentEntity.findAssociation("up_")).thenReturn(Optional.of(assoc));
	}

	@Test
	void storageCalledWithAllFieldsFilledFromPath() {
		var attachment = prepareAndExecuteEventWithData();

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.attachmentIds()).containsEntry("ID", attachment.getId());
		assertThat(resultValue.attachmentEntity()).isEqualTo(attachmentEntity);
		assertThat(resultValue.mimeType()).isEqualTo(attachment.getMimeType());
		assertThat(resultValue.fileName()).isEqualTo(attachment.getFileName());
		assertThat(resultValue.content()).isEqualTo(attachment.getContent());
	}

	@Test
	void storageCalledWithAllFieldsFilledFromExistingData() {
		var attachment = Attachments.create();

		attachment.setContent(mock(InputStream.class));
		attachment.setId(UUID.randomUUID().toString());
		attachment.put("up__ID", "test");

		when(target.values()).thenReturn(attachment);
		when(target.keys()).thenReturn(Map.of("ID", attachment.getId(), "up__ID", "test"));
		when(attachmentService.createAttachment(any()))
				.thenReturn(new AttachmentModificationResult(false, "id", "test"));
		var existingData = Attachments.create();
		existingData.setFileName("some file name");
		existingData.setMimeType("some mime type");

		cut.processEvent(path, attachment.getContent(), existingData, eventContext);

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var createInput = contextArgumentCaptor.getValue();
		assertThat(createInput.attachmentIds()).hasSize(2).containsEntry("ID", attachment.getId())
				.containsEntry("up__ID", "test");
		assertThat(createInput.attachmentEntity()).isEqualTo(attachmentEntity);
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

		cut.processEvent(path, attachment.getContent(), Attachments.create(), eventContext);

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
		when(attachmentService.createAttachment(any()))
				.thenReturn(new AttachmentModificationResult(false, contentId, "test"));

		cut.processEvent(path, null, Attachments.create(), eventContext);

		verify(changeSetContext).register(listener);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void contentIsReturnedIfNotExternalStored(boolean isExternalStored) throws IOException {
		var attachment = Attachments.create();

		var testContent = "test content";
		try (var testContentStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testContentStream);
			attachment.setId(UUID.randomUUID().toString());
		}
		when(target.values()).thenReturn(attachment);
		when(attachmentService.createAttachment(any()))
				.thenReturn(new AttachmentModificationResult(isExternalStored, "id", "test"));

		var result = cut.processEvent(path, attachment.getContent(), Attachments.create(), eventContext);

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
		when(attachmentService.createAttachment(any()))
				.thenReturn(new AttachmentModificationResult(false, "id", "test"));

		cut.processEvent(path, attachment.getContent(), Attachments.create(), eventContext);
		return attachment;
	}

	@Test
	void testGetParentAssocType() {
		var ref1 = mock(CqnElementRef.class);
		when(ref1.path()).thenReturn("ID");
		var ref2 = mock(CqnElementRef.class);
		when(ref2.path()).thenReturn("ID2");

		var assocType = mock(CdsAssociationType.class);
		when(assocType.refs()).thenReturn(Stream.of(ref1, ref2));

		var association = mock(CdsElement.class);
		when(association.getType()).thenReturn(assocType);

		// entity with up_ association
		when(attachmentEntity.findAssociation("up_")).thenReturn(Optional.of(association));
		when(attachmentEntity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
		var parentEntityWithUp = mock(CdsEntity.class);
		when(parentEntityWithUp.getQualifiedName()).thenReturn(PARENT_FULL_NAME);
		when(parentEntityWithUp.findAssociation("up_")).thenReturn(Optional.of(association));

		var attachment = Attachments.create();
		attachment.put("up__ID", "test");

		var parentAssocType = CreateAttachmentEvent.getParentAssocType(parentEntityWithUp);
		assertThat(parentAssocType).isNotNull();

		var parentIds = CreateAttachmentEvent.getParentIds(attachment, parentAssocType);
		assertThat(parentIds).hasSize(1);

		// entity without the up_ association
		var parentEntityWithoutUp = mock(CdsEntity.class);
		when(parentEntityWithoutUp.getQualifiedName()).thenReturn(PARENT_FULL_NAME);
		when(parentEntityWithoutUp.findAssociation("up_")).thenReturn(Optional.empty());

		parentAssocType = CreateAttachmentEvent.getParentAssocType(parentEntityWithoutUp);
		assertThat(parentAssocType).isNull();
	}

	@Test
	void testProcessEventWithoutUpAssoc() {
		when(attachmentService.createAttachment(any()))
				.thenReturn(new AttachmentModificationResult(false, "id", StatusCode.CLEAN));
		when(attachmentEntity.findAssociation("up_")).thenReturn(Optional.empty());
		cut.processEvent(path, null, Attachments.create(), eventContext);

		verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
		var resultValue = contextArgumentCaptor.getValue();
		assertThat(resultValue.parentIds()).isEmpty();
		assertThat(resultValue.parentEntity()).isNull();
	}
}

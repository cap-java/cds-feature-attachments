package com.sap.cds.feature.attachments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;
import com.sap.cds.services.handler.Handler;
import com.sap.cds.services.impl.ServiceSPI;
import com.sap.cds.services.runtime.CdsRuntime;

class DefaultAttachmentsServiceTest {

	private DefaultAttachmentsService cut;
	private Handler handler;
	private ServiceSPI serviceSpi;

	@BeforeEach
	void setup() {
		cut = new DefaultAttachmentsService();

		CdsRuntime runtime = mock(CdsRuntime.class);
		handler = mock(Handler.class);
		serviceSpi = (ServiceSPI) cut.getDelegatedService();
		serviceSpi.setCdsRuntime(runtime);

	}

	@Test
	void readAttachmentInsertsData() {
		var contextReference = new AtomicReference<AttachmentReadEventContext>();
		var stream = mock(InputStream.class);
		doAnswer(input -> {
			var context = (AttachmentReadEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			context.getData().setContent(stream);
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_READ_ATTACHMENT, "", handler);
		var documentId = "some id";

		var result = cut.readAttachment(documentId);

		assertThat(contextReference.get().getDocumentId()).isEqualTo(documentId);
		assertThat(result).isEqualTo(stream);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@NullSource
	void createAttachmentInsertsData(Boolean isExternalCreated) {
		var contextReference = new AtomicReference<AttachmentCreateEventContext>();
		var documentId = "some id";
		doAnswer(input -> {
			var context = (AttachmentCreateEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			context.setIsExternalCreated(isExternalCreated);
			context.setDocumentId(documentId);
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_CREATE_ATTACHMENT, "", handler);
		var stream = mock(InputStream.class);
		Map<String, Object> ids = Map.of("ID1", "value1", "id2", "Value2");
		var input = new CreateAttachmentInput(ids, "entityName", "fileName", "mimeType", stream);

		var result = cut.createAttachment(input);

		assertThat(result.isExternalStored()).isEqualTo(Objects.nonNull(isExternalCreated) ? isExternalCreated : false);
		assertThat(result.documentId()).isEqualTo(documentId);
		var createContext = contextReference.get();
		assertThat(createContext.getAttachmentIds()).isEqualTo(input.attachmentIds());
		assertThat(createContext.getAttachmentEntityName()).isEqualTo(input.attachmentEntityName());
		assertThat(createContext.getData().getFileName()).isEqualTo(input.fileName());
		assertThat(createContext.getData().getMimeType()).isEqualTo(input.mimeType());
		assertThat(createContext.getData().getContent()).isEqualTo(stream);
	}

	@Test
	void createAttachmentExternalCreateNotFilledReturnedFalse() {
		doAnswer(input -> {
			var context = (AttachmentCreateEventContext) input.getArgument(0);
			context.setCompleted();
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_CREATE_ATTACHMENT, "", handler);
		Map<String, Object> ids = Map.of("ID1", "value1", "id2", "Value2");
		var input = new CreateAttachmentInput(ids, "entityName", "fileName", "mimeType", mock(InputStream.class));

		var result = cut.createAttachment(input);

		assertThat(result.isExternalStored()).isFalse();
	}

	@Test
	void updateAttachmentInsertsData() {
		var contextReference = new AtomicReference<AttachmentUpdateEventContext>();
		var documentId = "some id";
		doAnswer(input -> {
			var context = (AttachmentUpdateEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_UPDATE_ATTACHMENT, "", handler);
		var stream = mock(InputStream.class);
		Map<String, Object> ids = Map.of("ID1", "value1", "id2", "Value2");
		var input = new UpdateAttachmentInput(documentId, ids, "entityName", "fileName", "mimeType", stream);

		var result = cut.updateAttachment(input);

		assertThat(result.documentId()).isEqualTo(documentId);
		var updateContext = contextReference.get();
		assertThat(updateContext.getAttachmentIds()).isEqualTo(input.attachmentIds());
		assertThat(updateContext.getAttachmentEntityName()).isEqualTo(input.attachmentEntityName());
		assertThat(updateContext.getData().getFileName()).isEqualTo(input.fileName());
		assertThat(updateContext.getData().getMimeType()).isEqualTo(input.mimeType());
		assertThat(updateContext.getData().getContent()).isEqualTo(stream);
	}

	@Test
	void deleteAttachmentInsertsData() {
		var contextReference = new AtomicReference<AttachmentDeleteEventContext>();
		var documentId = "some id";
		doAnswer(input -> {
			var context = (AttachmentDeleteEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_DELETE_ATTACHMENT, "", handler);

		cut.deleteAttachment(documentId);

		var deleteEventContext = contextReference.get();
		assertThat(deleteEventContext.getDocumentId()).isEqualTo(documentId);
	}

}

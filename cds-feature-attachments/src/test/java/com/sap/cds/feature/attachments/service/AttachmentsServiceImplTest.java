package com.sap.cds.feature.attachments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.handler.Handler;
import com.sap.cds.services.impl.ServiceSPI;
import com.sap.cds.services.runtime.CdsRuntime;

class AttachmentsServiceImplTest {

	private AttachmentsServiceImpl cut;
	private Handler handler;
	private ServiceSPI serviceSpi;

	@BeforeEach
	void setup() {
		cut = new AttachmentsServiceImpl();

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
		var contentId = "some id";

		var result = cut.readAttachment(contentId);

		assertThat(contextReference.get().getContentId()).isEqualTo(contentId);
		assertThat(result).isEqualTo(stream);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@NullSource
	void createAttachmentInsertsData(Boolean isExternalCreated) {
		var contextReference = new AtomicReference<AttachmentCreateEventContext>();
		var contentId = "some id";
		doAnswer(input -> {
			var context = (AttachmentCreateEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			context.setIsInternalStored(isExternalCreated);
			context.setContentId(contentId);
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_CREATE_ATTACHMENT, "", handler);
		var stream = mock(InputStream.class);
		Map<String, Object> ids = Map.of("ID1", "value1", "id2", "Value2");
		var input = new CreateAttachmentInput(ids, mock(CdsEntity.class), "fileName", "mimeType", stream);

		var result = cut.createAttachment(input);

		assertThat(result.isInternalStored()).isEqualTo(Boolean.TRUE.equals(isExternalCreated));
		assertThat(result.contentId()).isEqualTo(contentId);
		var createContext = contextReference.get();
		assertThat(createContext.getAttachmentIds()).isEqualTo(input.attachmentIds());
		assertThat(createContext.getAttachmentEntity()).isEqualTo(input.attachmentEntity());
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
		var input = new CreateAttachmentInput(ids, mock(CdsEntity.class), "fileName", "mimeType", mock(InputStream.class));

		var result = cut.createAttachment(input);

		assertThat(result.isInternalStored()).isFalse();
	}

	@Test
	void markAsDeleteAttachmentInsertsData() {
		var contextReference = new AtomicReference<AttachmentMarkAsDeletedEventContext>();
		var contentId = "some id";
		doAnswer(input -> {
			var context = (AttachmentMarkAsDeletedEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED, "", handler);

		cut.markAttachmentAsDeleted(contentId);

		var deleteEventContext = contextReference.get();
		assertThat(deleteEventContext.getContentId()).isEqualTo(contentId);
	}

	@Test
	void restoreAttachmentAttachmentInsertsData() {
		var contextReference = new AtomicReference<AttachmentRestoreEventContext>();
		doAnswer(input -> {
			var context = (AttachmentRestoreEventContext) input.getArgument(0);
			contextReference.set(context);
			context.setCompleted();
			return null;
		}).when(handler).process(any());
		serviceSpi.on(AttachmentService.EVENT_RESTORE_ATTACHMENT, "", handler);

		var timestamp = Instant.now();

		cut.restoreAttachment(timestamp);

		var deleteEventContext = contextReference.get();
		assertThat(deleteEventContext.getRestoreTimestamp()).isEqualTo(timestamp);
	}

}

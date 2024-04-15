package com.sap.cds.feature.attachments.service.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.generated.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreDeletedEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.impl.changeset.ChangeSetContextImpl;

class DefaultAttachmentsServiceHandlerTest {

	private static final int EXPECTED_HANDLER_ORDER = 11000;

	private DefaultAttachmentsServiceHandler cut;
	private EndTransactionMalwareScanProvider malwareScanProvider;

	@BeforeEach
	void setup() {
		malwareScanProvider = mock(EndTransactionMalwareScanProvider.class);
		cut = new DefaultAttachmentsServiceHandler(malwareScanProvider);
	}

	@AfterEach
	void tearDown() throws Exception {
		closeChangeSetContext();
	}

	@Test
	void createAttachmentsSetData() {
		var createContext = AttachmentCreateEventContext.create();
		var attachmentId = "test ID";
		createContext.setAttachmentIds(Map.of(Attachments.ID, attachmentId, "OtherId", "OtherID value"));
		createContext.setData(MediaData.create());
		createContext.setAttachmentEntity(mock(CdsEntity.class));
		ChangeSetContextImpl.open();

		cut.createAttachment(createContext);

		assertThat(createContext.isCompleted()).isTrue();
		assertThat(createContext.getDocumentId()).isEqualTo(attachmentId);
		assertThat(createContext.getIsInternalStored()).isTrue();
		assertThat(createContext.getData().getStatusCode()).isEqualTo(StatusCode.UNSCANNED);
	}

	@Test
	void deleteAttachmentSetData() {
		var deleteContext = AttachmentMarkAsDeletedEventContext.create();

		cut.markAttachmentAsDeleted(deleteContext);

		assertThat(deleteContext.isCompleted()).isTrue();
	}

	@Test
	void restoreDeleteAttachmentSetData() {
		var restoreContext = AttachmentRestoreDeletedEventContext.create();

		cut.restoreDeleteAttachment(restoreContext);

		assertThat(restoreContext.isCompleted()).isTrue();
	}

	@Test
	void readAttachmentSetData() {
		var readContext = AttachmentReadEventContext.create();

		cut.readAttachment(readContext);

		assertThat(readContext.isCompleted()).isTrue();
	}

	@Test
	void classHasCorrectAnnotation() {
		var annotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(annotation.value()).containsOnly("*");
		assertThat(annotation.type()).containsOnly(AttachmentService.class);
	}

	@Test
	void createMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var createMethod = cut.getClass().getMethod("createAttachment", AttachmentCreateEventContext.class);
		var onAnnotation = createMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = createMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).containsOnly(AttachmentService.EVENT_CREATE_ATTACHMENT);
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void restoreMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var updateMethod = cut.getClass().getMethod("restoreDeleteAttachment", AttachmentRestoreDeletedEventContext.class);
		var onAnnotation = updateMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = updateMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).containsOnly(AttachmentService.EVENT_RESTORE_DELETED);
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void deleteMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var deleteMethod = cut.getClass().getMethod("markAttachmentAsDeleted", AttachmentMarkAsDeletedEventContext.class);
		var onAnnotation = deleteMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = deleteMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).containsOnly(AttachmentService.EVENT_MARK_AS_DELETED);
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void readMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var readMethod = cut.getClass().getMethod("readAttachment", AttachmentReadEventContext.class);
		var onAnnotation = readMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = readMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).containsOnly(AttachmentService.EVENT_READ_ATTACHMENT);
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void malwareScannerRegisteredForEndOfTransaction() {
		var listener = mock(ChangeSetListener.class);
		var entity = mock(CdsEntity.class);
		when(malwareScanProvider.getChangeSetListener(entity, "documentId")).thenReturn(listener);
		var createContext = AttachmentCreateEventContext.create();
		createContext.setAttachmentIds(Map.of(Attachments.ID, "documentId"));
		createContext.setData(MediaData.create());
		createContext.setAttachmentEntity(entity);
		ChangeSetContextImpl.open();

		cut.createAttachment(createContext);

		verify(malwareScanProvider).getChangeSetListener(entity, "documentId");
	}

	private void closeChangeSetContext() throws Exception {
		var context = ChangeSetContextImpl.getCurrent();
		if (Objects.nonNull(context)) {
			try {
				context.close();
			} catch (RuntimeException ignored) {
				// ignore
			}
		}
	}


}

package com.sap.cds.feature.attachments.service.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.impl.changeset.ChangeSetContextImpl;

class AttachmentsServiceImplHandlerTest {

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
		ChangeSetContextImpl.open(false);

		cut.createAttachment(createContext);

		assertThat(createContext.isCompleted()).isTrue();
		assertThat(createContext.getContentId()).isEqualTo(attachmentId);
		assertThat(createContext.getIsInternalStored()).isTrue();
		assertThat(createContext.getData().getStatus()).isEqualTo(StatusCode.SCANNING);
	}

	@Test
	void deleteAttachmentSetData() {
		var deleteContext = AttachmentMarkAsDeletedEventContext.create();

		cut.markAttachmentAsDeleted(deleteContext);

		assertThat(deleteContext.isCompleted()).isTrue();
	}

	@Test
	void restoreAttachmentAttachmentSetData() {
		var restoreContext = AttachmentRestoreEventContext.create();

		cut.restoreAttachment(restoreContext);

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

		assertThat(onAnnotation.event()).isEmpty();
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void restoreAttachmentMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var updateMethod = cut.getClass().getMethod("restoreAttachment", AttachmentRestoreEventContext.class);
		var onAnnotation = updateMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = updateMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).isEmpty();
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void deleteMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var deleteMethod = cut.getClass().getMethod("markAttachmentAsDeleted", AttachmentMarkAsDeletedEventContext.class);
		var onAnnotation = deleteMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = deleteMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).isEmpty();
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void readMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var readMethod = cut.getClass().getMethod("readAttachment", AttachmentReadEventContext.class);
		var onAnnotation = readMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = readMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).isEmpty();
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void malwareScannerRegisteredForEndOfTransaction() {
		var listener = mock(ChangeSetListener.class);
		var entity = mock(CdsEntity.class);
		when(malwareScanProvider.getChangeSetListener(entity, "contentId")).thenReturn(listener);
		var createContext = AttachmentCreateEventContext.create();
		createContext.setAttachmentIds(Map.of(Attachments.ID, "contentId"));
		createContext.setData(MediaData.create());
		createContext.setAttachmentEntity(entity);
		ChangeSetContextImpl.open(false);

		cut.createAttachment(createContext);

		verify(malwareScanProvider).getChangeSetListener(entity, "contentId");
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

package com.sap.cds.feature.attachments.service.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

class DefaultAttachmentsServiceHandlerTest {

	private static final int EXPECTED_HANDLER_ORDER = 11000;

	private DefaultAttachmentsServiceHandler cut;

	@BeforeEach
	void setup() {
		cut = new DefaultAttachmentsServiceHandler();
	}

	@Test
	void createAttachmentsSetData() {
		var createContext = AttachmentCreateEventContext.create();
		var attachmentId = "test ID";
		createContext.setAttachmentIds(Map.of(Attachments.ID, attachmentId, "OtherId", "OtherID value"));

		cut.createAttachment(createContext);

		assertThat(createContext.isCompleted()).isTrue();
		assertThat(createContext.getDocumentId()).isEqualTo(attachmentId);
		assertThat(createContext.getIsExternalCreated()).isFalse();
	}

	@Test
	void updateAttachmentsSetData() {
		var updateContext = AttachmentUpdateEventContext.create();
		var documentId = "test document ID";
		updateContext.setAttachmentIds(Map.of(Attachments.ID, "test ID", "OtherId", "OtherID value"));
		updateContext.setDocumentId(documentId);

		cut.updateAttachment(updateContext);

		assertThat(updateContext.isCompleted()).isTrue();
		assertThat(updateContext.getDocumentId()).isEqualTo(documentId);
		assertThat(updateContext.getIsExternalCreated()).isFalse();
	}

	@Test
	void deleteAttachmentSetData() {
		var deleteContext = AttachmentDeleteEventContext.create();

		cut.deleteAttachment(deleteContext);

		assertThat(deleteContext.isCompleted()).isTrue();
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
	void updateMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var updateMethod = cut.getClass().getMethod("updateAttachment", AttachmentUpdateEventContext.class);
		var onAnnotation = updateMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = updateMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).containsOnly(AttachmentService.EVENT_UPDATE_ATTACHMENT);
		assertThat(handlerOrderAnnotation.value()).isEqualTo(EXPECTED_HANDLER_ORDER);
	}

	@Test
	void deleteMethodHasCorrectAnnotation() throws NoSuchMethodException {
		var deleteMethod = cut.getClass().getMethod("deleteAttachment", AttachmentDeleteEventContext.class);
		var onAnnotation = deleteMethod.getAnnotation(On.class);
		var handlerOrderAnnotation = deleteMethod.getAnnotation(HandlerOrder.class);

		assertThat(onAnnotation.event()).containsOnly(AttachmentService.EVENT_DELETE_ATTACHMENT);
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

}

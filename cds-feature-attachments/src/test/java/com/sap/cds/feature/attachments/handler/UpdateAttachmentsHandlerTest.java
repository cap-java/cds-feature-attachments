package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.WrongAttachment;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.WrongAttachment_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

class UpdateAttachmentsHandlerTest extends ModifyApplicationEventTestBase {

	private UpdateAttachmentsHandler cut;

	private CdsUpdateEventContext updateContext;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		super.setup();
		cut = new UpdateAttachmentsHandler(persistenceService, eventFactory);

		updateContext = mock(CdsUpdateEventContext.class);
	}

	@Test
	void noContentInDataNothingToDo() {
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			var attachment = Attachments.create();
		mockTargetInContext(serviceEntity.orElseThrow());

		cut.processAfter(updateContext, List.of(attachment));

		verifyNoInteractions(persistenceService);
		verifyNoInteractions(eventFactory);
	}

	@Test
	void eventProcessorCalledForUpdate() throws IOException {
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);

		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
				var attachment = Attachments.create();
			attachment.setContent(testStream);
			attachment.setId("test");
			mockTargetInContext(serviceEntity.orElseThrow());
			when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
			var row = mockSelectionResult();

			cut.processAfter(updateContext, List.of(attachment));

			verify(eventFactory).getEvent(eq(CqnService.EVENT_UPDATE), eq(testStream), fieldNamesArgumentCaptor.capture(), eq(row));
			verifyFilledFieldNames();
		}
	}

	@Test
	void attachmentAccessExceptionCorrectHandledForUpdate() {
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			var attachment = Attachments.create();
		attachment.setFilename("test.txt");
		attachment.setContent(null);
		attachment.setId("some id");
		mockTargetInContext(serviceEntity.orElseThrow());
		when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
		when(event.processEvent(any(), any(), any(), any(), any(), any())).thenThrow(new ServiceException(""));
		mockSelectionResult();

		List<CdsData> input = List.of(attachment);
		assertThrows(ServiceException.class, () -> cut.processAfter(updateContext, input));
	}

	@Test
	void illegalStateExceptionIfIdNotProvidedForUpdate() {
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			var attachment = Attachments.create();
		attachment.setFilename("test.txt");
		attachment.setContent(null);
		mockTargetInContext(serviceEntity.orElseThrow());

		List<CdsData> input = List.of(attachment);
		assertThrows(IllegalStateException.class, () -> cut.processAfter(updateContext, input));
	}

	@Test
	void noExceptionIfAttachmentEntityWrongDefined() {
		var serviceEntity = runtime.getCdsModel().findEntity(WrongAttachment_.CDS_NAME);
		var attachment = WrongAttachment.create();
		attachment.setFilename("test.txt");
		attachment.setContent(null);
		attachment.setId(1);
		mockTargetInContext(serviceEntity.orElseThrow());
		when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
		var row = mockSelectionResult();

		List<CdsData> input = List.of(attachment);
		assertDoesNotThrow(() -> cut.processAfter(updateContext, input));

		verify(eventFactory).getEvent(eq(CqnService.EVENT_UPDATE), eq(null), fieldNamesArgumentCaptor.capture(), eq(row));
		verifyEmptyFieldNames();
	}

	@Test
	void classHasCorrectAnnotation() {
		var updateHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(updateHandlerAnnotation.type()).containsOnly(ApplicationService.class);
		assertThat(updateHandlerAnnotation.value()).containsOnly("*");
	}

	@Test
	void methodHasCorrectAnnotations() throws NoSuchMethodException {
		var method = cut.getClass().getMethod("processAfter", CdsUpdateEventContext.class, List.class);

		var updateAfterAnnotation = method.getAnnotation(After.class);
		var updateHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(updateAfterAnnotation.event()).containsOnly(CqnService.EVENT_UPDATE);
		assertThat(updateHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
	}

	private void mockTargetInContext(CdsEntity serviceEntity) {
		when(updateContext.getTarget()).thenReturn(serviceEntity);
	}

	private void verifyEmptyFieldNames() {
		//field names taken from model for entity WrongAttachments defined in csn which can be found in AttachmentsHandlerTestBase.CSN_FILE_PATH
		var fieldNames = fieldNamesArgumentCaptor.getValue();
		assertThat(fieldNames.keyField()).isEqualTo("ID");
		assertThat(fieldNames.documentIdField()).isEmpty();
		assertThat(fieldNames.mimeTypeField()).isEmpty();
		assertThat(fieldNames.fileNameField()).isEmpty();
	}

}

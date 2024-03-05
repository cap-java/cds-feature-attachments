package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.WrongAttachment;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.WrongAttachment_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

class UpdateAttachmentsHandlerTest extends ModifyApplicationEventTestBase {

	private UpdateAttachmentsHandler cut;
	private CdsUpdateEventContext updateContext;
	private ArgumentCaptor<CqnSelect> selectArgumentCaptor;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		super.setup();
		cut = new UpdateAttachmentsHandler(persistenceService, eventFactory);

		updateContext = mock(CdsUpdateEventContext.class);
		selectArgumentCaptor = ArgumentCaptor.forClass(CqnSelect.class);
	}

	@Test
	void noContentInDataNothingToDo() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();

		cut.processBefore(updateContext, List.of(attachment));

		verifyNoInteractions(persistenceService);
		verifyNoInteractions(eventFactory);
	}

	@Test
	void eventProcessorCalledForUpdate() throws IOException {
		getEntityAndMockContext(Attachment_.CDS_NAME);

		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
			var attachment = Attachments.create();
			attachment.setContent(testStream);
			attachment.setId("test");
			when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
			var row = mockSelectionResult();

			cut.processBefore(updateContext, List.of(attachment));

			verify(eventFactory).getEvent(CqnService.EVENT_UPDATE, testStream, row);
		}
	}

	@Test
	void attachmentAccessExceptionCorrectHandledForUpdate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();
		attachment.setFileName("test.txt");
		attachment.setContent(null);
		attachment.setId("some id");
		when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
		when(event.processEvent(any(), any(), any(), any(), any())).thenThrow(new ServiceException(""));
		mockSelectionResult();

		List<CdsData> input = List.of(attachment);
		assertThrows(ServiceException.class, () -> cut.processBefore(updateContext, input));
	}

	@Test
	void illegalStateExceptionIfIdNotProvidedForUpdate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();
		attachment.setFileName("test.txt");
		attachment.setContent(null);

		List<CdsData> input = List.of(attachment);
		assertThrows(IllegalStateException.class, () -> cut.processBefore(updateContext, input));
	}

	@Test
	void noExceptionIfAttachmentEntityWrongDefined() {
		getEntityAndMockContext(WrongAttachment_.CDS_NAME);
		var attachment = WrongAttachment.create();
		attachment.setFilename("test.txt");
		attachment.setContent(null);
		attachment.setId(1);
		when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
		var row = mockSelectionResult();

		List<CdsData> input = List.of(attachment);
		assertDoesNotThrow(() -> cut.processBefore(updateContext, input));

		verify(eventFactory).getEvent(CqnService.EVENT_UPDATE, null, row);
	}

	@Test
	void existingDataFoundAndUsed() throws IOException {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var row = mockSelectionResult();
		when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

		var root = RootTable.create();
		root.setId(UUID.randomUUID().toString());
		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.put("up__ID", root.getId());

		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
			attachment.setContent(testStream);
			root.setAttachmentTable(List.of(attachment));


			cut.processBefore(updateContext, List.of(root));

			verify(eventFactory).getEvent(CqnService.EVENT_UPDATE, testStream, row);
		}

		verify(persistenceService).run(selectArgumentCaptor.capture());
		var select = selectArgumentCaptor.getValue();
		assertThat(select.where().toString()).contains(attachment.getId());
		assertThat(select.where().toString()).contains(root.getId());
	}

	@Test
	void classHasCorrectAnnotation() {
		var updateHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(updateHandlerAnnotation.type()).containsOnly(ApplicationService.class);
		assertThat(updateHandlerAnnotation.value()).containsOnly("*");
	}

	@Test
	void methodHasCorrectAnnotations() throws NoSuchMethodException {
		var method = cut.getClass().getMethod("processBefore", CdsUpdateEventContext.class, List.class);

		var updateBeforeAnnotation = method.getAnnotation(Before.class);
		var updateHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(updateBeforeAnnotation.event()).containsOnly(CqnService.EVENT_UPDATE);
		assertThat(updateHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
	}

	@Test
	void correctAttachmentIdUsed() {
		//TODO check ID field is retrieved correct if other keys available
		fail("not implemented");
	}

	private void getEntityAndMockContext(String cdsName) {
		var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		mockTargetInUpdateContext(serviceEntity.orElseThrow());
	}

	private void mockTargetInUpdateContext(CdsEntity serviceEntity) {
		when(updateContext.getTarget()).thenReturn(serviceEntity);
	}

}

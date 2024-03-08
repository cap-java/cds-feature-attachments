package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

class CreateAttachmentsHandlerTest extends ModifyApplicationEventTestBase {

	private CreateAttachmentsHandler cut;
	private CdsCreateEventContext createContext;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		super.setup();
		cut = new CreateAttachmentsHandler(persistenceService, eventFactory);

		createContext = mock(CdsCreateEventContext.class);
	}

	@Test
	void noContentInDataNothingToDo() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();

		cut.processBefore(createContext, List.of(attachment));

		verifyNoInteractions(persistenceService);
		verifyNoInteractions(eventFactory);
	}

	@Test
	void idsAreSetInDataForCreate() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var roots = RootTable.create();
		var attachment = Attachments.create();
		attachment.setFileName("test.txt");
		attachment.setContent(null);
		attachment.put("up__ID", "test");
		roots.setAttachmentTable(List.of(attachment));
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any())).thenReturn(event);

		cut.processBefore(createContext, List.of(roots));

		assertThat(roots.getId()).isNotEmpty();
		assertThat(attachment.getId()).isNotEmpty();
	}

	@Test
	void eventProcessorCalledForCreate() throws IOException {
		getEntityAndMockContext(Attachment_.CDS_NAME);

		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
			var attachment = Attachments.create();
			attachment.setContent(testStream);
			when(eventFactory.getEvent(any(), any(), anyBoolean(), any())).thenReturn(event);
			mockSelectionResult();

			cut.processBefore(createContext, List.of(attachment));

			verify(eventFactory).getEvent(testStream, null, false, CdsData.create());
		}
	}

	@Test
	void attachmentAccessExceptionCorrectHandledForCreate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();
		attachment.setFileName("test.txt");
		attachment.setContent(null);
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any())).thenReturn(event);
		when(event.processEvent(any(), any(), any(), any(), any())).thenThrow(new ServiceException(""));

		List<CdsData> input = List.of(attachment);
		assertThrows(ServiceException.class, () -> cut.processBefore(createContext, input));
	}

	@Test
	void handlerCalledForMediaEventInAssociationIdsAreSet() {
		getEntityAndMockContext(Events_.CDS_NAME);
		var events = Events.create();
		events.setContent("test");
		var items = Items.create();
		var attachment = Attachments.create();
		attachment.setContent(mock(InputStream.class));
		items.setAttachments(List.of(attachment));
		events.setItems(List.of(items));
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any())).thenReturn(event);

		List<CdsData> input = List.of(events);
		cut.processBefore(createContext, input);

		assertThat(events.getId1()).isNotEmpty();
		assertThat(events.getId2()).isNull();
	}

	@Test
	void handlerCalledForNonMediaEventNothingSetAndCalled() {
		getEntityAndMockContext(Events_.CDS_NAME);
		var events = Events.create();
		events.setContent("test");
		var eventItems = Items.create();
		var attachment = Attachments.create();
		attachment.setContent(mock(InputStream.class));
		eventItems.setAttachments(List.of(attachment));
		events.setEventItems(List.of(eventItems));
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any())).thenReturn(event);

		List<CdsData> input = List.of(events);
		cut.processBefore(createContext, input);

		verifyNoInteractions(eventFactory);
		assertThat(events.getId1()).isNull();
		assertThat(events.getId2()).isNull();
	}

	@Test
	void classHasCorrectAnnotation() {
		var createHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(createHandlerAnnotation.type()).containsOnly(ApplicationService.class);
		assertThat(createHandlerAnnotation.value()).containsOnly("*");
	}

	@Test
	void methodHasCorrectAnnotations() throws NoSuchMethodException {
		var method = cut.getClass().getMethod("processBefore", CdsCreateEventContext.class, List.class);

		var createBeforeAnnotation = method.getAnnotation(Before.class);
		var createHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(createBeforeAnnotation.event()).containsOnly(CqnService.EVENT_CREATE);
		assertThat(createHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
	}

	private void getEntityAndMockContext(String cdsName) {
		var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		mockTargetInCreateContext(serviceEntity.orElseThrow());
	}

	private void mockTargetInCreateContext(CdsEntity serviceEntity) {
		when(createContext.getTarget()).thenReturn(serviceEntity);
	}

}

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
import com.sap.cds.feature.attachments.generated.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;

class CreateAttachmentsHandlerTest {

	private static CdsRuntime runtime;

	private CreateAttachmentsHandler cut;
	private ModifyAttachmentEventFactory eventFactory;
	private CdsCreateEventContext createContext;
	private ModifyAttachmentEvent event;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		eventFactory = mock(ModifyAttachmentEventFactory.class);
		cut = new CreateAttachmentsHandler(eventFactory);

		createContext = mock(CdsCreateEventContext.class);
		event = mock(ModifyAttachmentEvent.class);
	}

	@Test
	void noContentInDataNothingToDo() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();

		cut.processBefore(createContext, List.of(attachment));

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
		roots.setAttachments(List.of(attachment));
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(event);

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
			when(eventFactory.getEvent(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(event);

			cut.processBefore(createContext, List.of(attachment));

			verify(eventFactory).getEvent(testStream, null, false, CdsData.create(), false);
		}
	}

	@Test
	void attachmentAccessExceptionCorrectHandledForCreate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();
		attachment.setFileName("test.txt");
		attachment.setContent(null);
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(event);
		when(event.processEvent(any(), any(), any(), any())).thenThrow(new ServiceException(""));

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
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(event);

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
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any(), anyBoolean())).thenReturn(event);

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

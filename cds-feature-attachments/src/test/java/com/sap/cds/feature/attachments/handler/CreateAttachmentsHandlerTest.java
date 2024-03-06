package com.sap.cds.feature.attachments.handler;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CdsCreateEventContext;

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

	//	@Test
//	void idsAreSetInDataForCreate() {
//		getEntityAndMockContext(RootTable_.CDS_NAME);
//		var roots = RootTable.create();
//		var attachment = Attachments.create();
//		attachment.setFileName("test.txt");
//		attachment.setContent(null);
//		attachment.put("up__ID", "test");
//		roots.setAttachmentTable(List.of(attachment));
//		when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
//
//		cut.processBefore(createContext, List.of(roots));
//
//		assertThat(roots.getId()).isNotEmpty();
//		assertThat(attachment.getId()).isNotEmpty();
//	}
//
//	@Test
//	void eventProcessorCalledForCreate() throws IOException {
//		getEntityAndMockContext(Attachment_.CDS_NAME);
//
//		try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
//			var attachment = Attachments.create();
//			attachment.setContent(testStream);
//			when(eventFactory.getEvent(any(), any(), any(), any())).thenReturn(event);
//			var row = mockSelectionResult();
//
//			cut.processBefore(createContext, List.of(attachment));
//
//			verify(eventFactory).getEvent(CqnService.EVENT_CREATE, testStream, row);
//		}
//	}
//
//	@Test
//	void attachmentAccessExceptionCorrectHandledForCreate() {
//		getEntityAndMockContext(Attachment_.CDS_NAME);
//		var attachment = Attachments.create();
//		attachment.setFileName("test.txt");
//		attachment.setContent(null);
//		when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
//		when(event.processEvent(any(), any(), any(), any(), any())).thenThrow(new ServiceException(""));
//
//		List<CdsData> input = List.of(attachment);
//		assertThrows(ServiceException.class, () -> cut.processBefore(createContext, input));
//	}
//
//	@Test
//	void classHasCorrectAnnotation() {
//		var createHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);
//
//		assertThat(createHandlerAnnotation.type()).containsOnly(ApplicationService.class);
//		assertThat(createHandlerAnnotation.value()).containsOnly("*");
//	}
//
//	@Test
//	void methodHasCorrectAnnotations() throws NoSuchMethodException {
//		var method = cut.getClass().getMethod("processBefore", CdsCreateEventContext.class, List.class);
//
//		var createBeforeAnnotation = method.getAnnotation(Before.class);
//		var createHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);
//
//		assertThat(createBeforeAnnotation.event()).containsOnly(CqnService.EVENT_CREATE);
//		assertThat(createHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
//	}
//
	private void getEntityAndMockContext(String cdsName) {
		var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		mockTargetInCreateContext(serviceEntity.orElseThrow());
	}

	private void mockTargetInCreateContext(CdsEntity serviceEntity) {
		when(createContext.getTarget()).thenReturn(serviceEntity);
	}

}

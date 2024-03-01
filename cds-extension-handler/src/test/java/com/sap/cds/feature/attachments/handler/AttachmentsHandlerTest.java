package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.ApplicationEventFactory;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

class AttachmentsHandlerTest {

		private AttachmentsHandler cut;
		private ApplicationEventFactory eventProcessor;
		private EventContext context;
		private CdsReadEventContext readContext;
		private List<CdsData> data;
		private ApplicationEvent applicationEvent;

		@BeforeEach
		void setup() {
				eventProcessor = mock(ApplicationEventFactory.class);
				cut = new AttachmentsHandler(eventProcessor);

				context = mock(EventContext.class);
				readContext = mock(CdsReadEventContext.class);
				data = List.of(CdsData.create());
				applicationEvent = mock(ApplicationEvent.class);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void processingCalledIfRelevant(String event) {
				when(context.getEvent()).thenReturn(event);
				when(eventProcessor.getApplicationEvent(event)).thenReturn(applicationEvent);

				cut.uploadAttachments(context, data);

				verify(applicationEvent).processAfter(context, data);
		}

		@Test
		void processBeforeReadEvent() {
				when(readContext.getEvent()).thenReturn(CqnService.EVENT_READ);
				when(eventProcessor.getApplicationEvent(CqnService.EVENT_READ)).thenReturn(applicationEvent);

				cut.readAttachmentsBeforeEvent(readContext);

				verify(applicationEvent).processBefore(readContext);
		}

		@Test
		void processAfterReadEvent() {
				when(readContext.getEvent()).thenReturn(CqnService.EVENT_READ);
				when(eventProcessor.getApplicationEvent(CqnService.EVENT_READ)).thenReturn(applicationEvent);

				cut.readAttachmentsAfterEvent(readContext, data);

				verify(applicationEvent).processAfter(readContext, data);
		}

		@Test
		void classHasCorrectAnnotation() {
				var annotation = cut.getClass().getAnnotation(ServiceName.class);

				assertThat(annotation.value()).contains("*");
				assertThat(annotation.type()).contains(ApplicationService.class);
		}

		@Test
		void readBeforeEventMethodHasCorrectAnnotation() throws NoSuchMethodException {
				var method = cut.getClass().getMethod("readAttachmentsBeforeEvent", CdsReadEventContext.class);
				var beforeAnnotation = method.getAnnotation(Before.class);
				var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

				assertThat(beforeAnnotation).isNotNull();
				assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
		}

		@Test
		void readAfterEventMethodHasCorrectAnnotation() throws NoSuchMethodException {
				var method = cut.getClass().getMethod("readAttachmentsAfterEvent", CdsReadEventContext.class, List.class);
				var afterAnnotation = method.getAnnotation(After.class);
				var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

				assertThat(afterAnnotation).isNotNull();
				assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
		}

		@Test
		void uploadAttachmentEventMethodHasCorrectAnnotation() throws NoSuchMethodException {
				var method = cut.getClass().getMethod("uploadAttachments", EventContext.class, List.class);
				var beforeAnnotation = method.getAnnotation(Before.class);
				var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

				assertThat(beforeAnnotation.event()).hasSize(2).contains(CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE);
				assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
		}

}

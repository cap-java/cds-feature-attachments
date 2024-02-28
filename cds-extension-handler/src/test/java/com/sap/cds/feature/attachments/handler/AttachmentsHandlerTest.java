package com.sap.cds.feature.attachments.handler;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.ApplicationEventFactory;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.CqnService;

class AttachmentsHandlerTest {

		private AttachmentsHandler cut;
		private ApplicationEventFactory eventProcessor;
		private EventContext context;
		private List<CdsData> data;

		@BeforeEach
		void setup() {
				eventProcessor = mock(ApplicationEventFactory.class);
				cut = new AttachmentsHandler(eventProcessor);

				context = mock(EventContext.class);
				data = List.of(CdsData.create());
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void processingCalledIfRelevant(String event) {
				when(context.getEvent()).thenReturn(event);
				var eventImplementation = mock(ApplicationEvent.class);
				when(eventProcessor.getApplicationEvent(event)).thenReturn(eventImplementation);

				cut.uploadAttachments(context, data);

				verify(eventImplementation).process(context, data);
		}

}

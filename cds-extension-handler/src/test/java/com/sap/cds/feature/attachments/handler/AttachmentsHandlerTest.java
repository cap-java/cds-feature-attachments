package com.sap.cds.feature.attachments.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.ApplicationEventProcessor;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.CqnService;

class AttachmentsHandlerTest {

		private AttachmentsHandler cut;
		private ApplicationEventProcessor eventProcessor;
		private EventContext context;
		private CdsEntity target;
		private List<CdsData> data;

		@BeforeEach
		void setup() {
				eventProcessor = mock(ApplicationEventProcessor.class);
				cut = new AttachmentsHandler(eventProcessor);

				context = mock(EventContext.class);
				target = mock(CdsEntity.class);
				when(context.getTarget()).thenReturn(target);
				data = List.of(CdsData.create());
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void processingNotCalledIfNotRelevant(String event) {
				when(eventProcessor.isAttachmentEvent(target, data)).thenReturn(false);
				when(context.getEvent()).thenReturn(event);

				cut.uploadAttachments(context, data);

				verify(eventProcessor).isAttachmentEvent(target, data);
				verifyNoMoreInteractions(eventProcessor);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void processingCalledIfRelevant(String event) {
				when(eventProcessor.isAttachmentEvent(target, data)).thenReturn(true);
				when(context.getEvent()).thenReturn(event);
				var eventImplementation = mock(ApplicationEvent.class);
				when(eventProcessor.getApplicationEvent(event)).thenReturn(eventImplementation);

				cut.uploadAttachments(context, data);

				verify(eventProcessor).isAttachmentEvent(target, data);
				verify(eventImplementation).process(context, data);
		}

}

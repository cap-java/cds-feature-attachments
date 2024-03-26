package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.services.EventContext;

class UpdateAttachmentEventTest {

	private UpdateAttachmentEvent cut;
	private ModifyAttachmentEvent createEvent;
	private ModifyAttachmentEvent deleteEvent;
	private Path path;
	private CdsElement element;

	@BeforeEach
	void setup() {
		createEvent = mock(ModifyAttachmentEvent.class);
		deleteEvent = mock(ModifyAttachmentEvent.class);

		cut = new UpdateAttachmentEvent(createEvent, deleteEvent);

		path = mock(Path.class);
		element = mock(CdsElement.class);
	}

	@Test
	void eventsCorrectCalled() {
		var testContentStream = mock(InputStream.class);
		var existingData = CdsData.create();
		Map<String, Object> keys = Map.of("ID", UUID.randomUUID().toString());
		var eventContext = mock(EventContext.class);

		cut.processEvent(path, element, testContentStream, existingData, keys, eventContext);

		verify(createEvent).processEvent(path, element, testContentStream, existingData, keys, eventContext);
		verify(deleteEvent).processEvent(path, element, testContentStream, existingData, keys, eventContext);
	}

}

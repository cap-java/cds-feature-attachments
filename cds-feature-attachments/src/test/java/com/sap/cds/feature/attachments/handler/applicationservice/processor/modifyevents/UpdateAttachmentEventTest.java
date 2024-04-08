package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static org.mockito.Mockito.*;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

class UpdateAttachmentEventTest {

	private UpdateAttachmentEvent cut;
	private ModifyAttachmentEvent createEvent;
	private ModifyAttachmentEvent deleteEvent;
	private Path path;

	@BeforeEach
	void setup() {
		createEvent = mock(ModifyAttachmentEvent.class);
		deleteEvent = mock(ModifyAttachmentEvent.class);

		cut = new UpdateAttachmentEvent(createEvent, deleteEvent);

		path = mock(Path.class);
		var target = mock(ResolvedSegment.class);
		when(path.target()).thenReturn(target);
		var entity = mock(CdsEntity.class);
		when(target.entity()).thenReturn(entity);
		when(entity.getQualifiedName()).thenReturn("some.qualified.name");
	}

	@Test
	void eventsCorrectCalled() {
		var testContentStream = mock(InputStream.class);
		var existingData = CdsData.create();
		var eventContext = mock(EventContext.class);

		cut.processEvent(path, testContentStream, existingData, eventContext);

		verify(createEvent).processEvent(path, testContentStream, existingData, eventContext);
		verify(deleteEvent).processEvent(path, testContentStream, existingData, eventContext);
	}

}

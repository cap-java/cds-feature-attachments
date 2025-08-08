/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

class UpdateAttachmentEventTest {

	private UpdateAttachmentEvent cut;
	private CreateAttachmentEvent createEvent;
	private MarkAsDeletedAttachmentEvent deleteEvent;
	private Path path;

	@BeforeEach
	void setup() {
		createEvent = mock(CreateAttachmentEvent.class);
		deleteEvent = mock(MarkAsDeletedAttachmentEvent.class);

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
		var existingData = Attachments.create();
		var eventContext = mock(EventContext.class);

		cut.processEvent(path, testContentStream, existingData, eventContext);

		verify(createEvent).processEvent(path, testContentStream, existingData, eventContext);
		verify(deleteEvent).processEvent(path, testContentStream, existingData, eventContext);
	}

}

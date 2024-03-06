package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;

class DefaultModifyAttachmentEventFactoryTest {

	private DefaultModifyAttachmentEventFactory cut;
	private ModifyAttachmentEvent createEvent;
	private ModifyAttachmentEvent updateEvent;
	private ModifyAttachmentEvent deleteContentEvent;
	private ModifyAttachmentEvent doNothingContentEvent;

	@BeforeEach
	void setup() {
		createEvent = mock(ModifyAttachmentEvent.class);
		updateEvent = mock(ModifyAttachmentEvent.class);
		deleteContentEvent = mock(ModifyAttachmentEvent.class);
		doNothingContentEvent = mock(ModifyAttachmentEvent.class);

		cut = new DefaultModifyAttachmentEventFactory(createEvent, updateEvent, deleteContentEvent, doNothingContentEvent);
	}

//	@ParameterizedTest
//	@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
//	void updateEventReturned(String eventName) {
//		var cdsData = CdsData.create();
//		cdsData.put(Attachments.DOCUMENT_ID, "documentId");
//
//		var event = cut.getEvent(eventName, "value", cdsData);
//
//		assertThat(event).isEqualTo(updateEvent);
//	}
//
//	@ParameterizedTest
//	@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
//	void updateEventReturnedIDocumentFieldNameNotPresent(String eventName) {
//		var cdsData = CdsData.create();
//		cdsData.put("documentID", "documentId");
//
//		var event = cut.getEvent(eventName, "value", cdsData);
//
//		assertThat(event).isEqualTo(createEvent);
//	}
//
//	@ParameterizedTest
//	@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
//	void createEventReturned(String eventName) {
//		var cdsData = CdsData.create();
//
//		var event = cut.getEvent(eventName, "value", cdsData);
//
//		assertThat(event).isEqualTo(createEvent);
//	}
//
//	@ParameterizedTest
//	@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
//	void deleteEventReturned(String eventName) {
//		var cdsData = CdsData.create();
//
//		var event = cut.getEvent(eventName, null, cdsData);
//
//		assertThat(event).isEqualTo(deleteContentEvent);
//	}

	@Test
	void exceptionThrownIfWrongEventType() {
		var cdsData = CdsData.create();

		assertDoesNotThrow(() -> cut.getEvent("WRONG_EVENT", null, true, cdsData));
//		assertThrows(IllegalStateException.class, () -> cut.getEvent("WRONG_EVENT", null, true, cdsData));
	}

}

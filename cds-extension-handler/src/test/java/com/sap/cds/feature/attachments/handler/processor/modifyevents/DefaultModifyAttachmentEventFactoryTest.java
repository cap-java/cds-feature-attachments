package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.services.cds.CqnService;

class DefaultModifyAttachmentEventFactoryTest {

		private DefaultModifyAttachmentEventFactory cut;
		private ModifyAttachmentEvent createEvent;
		private ModifyAttachmentEvent updateEvent;
		private ModifyAttachmentEvent deleteContentEvent;


		@BeforeEach
		void setup() {
				createEvent = mock(ModifyAttachmentEvent.class);
				updateEvent = mock(ModifyAttachmentEvent.class);
				deleteContentEvent = mock(ModifyAttachmentEvent.class);

				cut = new DefaultModifyAttachmentEventFactory(createEvent, updateEvent, deleteContentEvent);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void updateEventReturned(String eventName) {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();
				cdsData.put(fieldNames.documentIdField().orElseThrow(), "documentId");

				var event = cut.getEvent(eventName, "value", fieldNames, cdsData);

				assertThat(event).isEqualTo(updateEvent);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void updateEventReturnedIDocumentFieldNameNotPresent(String eventName) {
				var fieldNames = new AttachmentFieldNames("key", Optional.empty(), Optional.of("mimeType"), Optional.of("fileName"));
				var cdsData = CdsData.create();
				cdsData.put("documentID", "documentId");

				var event = cut.getEvent(eventName, "value", fieldNames, cdsData);

				assertThat(event).isEqualTo(createEvent);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void storeEventReturned(String eventName) {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();

				var event = cut.getEvent(eventName, "value", fieldNames, cdsData);

				assertThat(event).isEqualTo(createEvent);
		}

		@ParameterizedTest
		@ValueSource(strings = {CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE})
		void deleteEventReturned(String eventName) {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();

				var event = cut.getEvent(eventName, null, fieldNames, cdsData);

				assertThat(event).isEqualTo(deleteContentEvent);
		}

		@Test
		void exceptionThrownIfWrongEventType() {
				var fieldNames = getDefaultFieldNames();
				var cdsData = CdsData.create();

				assertThrows(IllegalStateException.class, () -> cut.getEvent("WRONG_EVENT", null, fieldNames, cdsData));
		}

		private AttachmentFieldNames getDefaultFieldNames() {
				return new AttachmentFieldNames("key", Optional.of("documentID"), Optional.of("mimeType"), Optional.of("fileName"));
		}

}

package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;

class DefaultModifyAttachmentEventFactoryTest {

	private DefaultModifyAttachmentEventFactory cut;
	private ModifyAttachmentEvent createEvent;
	private ModifyAttachmentEvent updateEvent;
	private ModifyAttachmentEvent deleteContentEvent;
	private ModifyAttachmentEvent doNothingEvent;

	@BeforeEach
	void setup() {
		createEvent = mock(ModifyAttachmentEvent.class);
		updateEvent = mock(ModifyAttachmentEvent.class);
		deleteContentEvent = mock(ModifyAttachmentEvent.class);
		doNothingEvent = mock(ModifyAttachmentEvent.class);

		cut = new DefaultModifyAttachmentEventFactory(createEvent, updateEvent, deleteContentEvent, doNothingEvent);
	}

	@Test
	void allNullNothingToDo() {
		var event = cut.getEvent(null, null, true, CdsData.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@Test
	void documentIdsNullContentFilledReturnedCreateEvent() {
		var event = cut.getEvent(mock(InputStream.class), null, true, CdsData.create());

		assertThat(event).isEqualTo(createEvent);
	}

	@Test
	void documentIdNullButtExistingNotNullReturnsUpdate() {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, null, true, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@Test
	void documentIdNullButtExistingNotNullReturnsDelete() {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), null, true, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@Test
	void documentIdsSameContentFillReturnsUpdate() {
		var documentId = "test ID";
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, documentId);

		var event = cut.getEvent(mock(InputStream.class), documentId, true, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@Test
	void documentIdsSameContentNullReturnsNothingToDo() {
		var documentId = "test ID";
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, documentId);

		var event = cut.getEvent(null, documentId, true, data);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNotNullReturnsUpdateEvent(String documentId) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), documentId, false, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNotNullReturnsDeleteEvent(String documentId) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, documentId, false, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNullReturnsCreateEvent(String documentId) {
		var event = cut.getEvent(mock(InputStream.class), documentId, false, CdsData.create());

		assertThat(event).isEqualTo(createEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNullReturnsDoNothingEvent(String documentId) {
		var event = cut.getEvent(null, documentId, false, CdsData.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

}

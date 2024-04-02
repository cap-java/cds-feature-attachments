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

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void allNullNothingToDo(boolean isDraftEntity) {
		var event = cut.getEvent(null, null, true, CdsData.create(), isDraftEntity);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void documentIdsNullContentFilledReturnedCreateEvent(boolean isDraftEntity) {
		var event = cut.getEvent(mock(InputStream.class), null, true, CdsData.create(), isDraftEntity);

		assertThat(event).isEqualTo(createEvent);
	}

	@Test
	void documentIdNullButtExistingNotNullReturnsDelete() {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, null, true, data, false);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@Test
	void documentIdPresentAndExistingNotNullButContentNullReturnsNothingToDoEventInCaseOfDraft() {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, null, true, data, true);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@Test
	void documentIdNullButtExistingNotNullReturnsNothingToDoInCaseOfDraft() {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, null, true, data, true);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void documentIdNullButtExistingNotNullReturnsDelete(boolean isDraftEntity) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), null, true, data, isDraftEntity);

		assertThat(event).isEqualTo(updateEvent);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void documentIdsSameContentFillReturnsUpdate(boolean isDraftEntity) {
		var documentId = "test ID";
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, documentId);

		var event = cut.getEvent(mock(InputStream.class), documentId, true, data, isDraftEntity);

		assertThat(event).isEqualTo(updateEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNotNullReturnsUpdateEvent(String documentId) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), documentId, false, data, false);

		assertThat(event).isEqualTo(updateEvent);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void documentIdsSameContentNullReturnsNothingToDo(boolean isDraftEntity) {
		var documentId = "test ID";
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, documentId);

		var event = cut.getEvent(null, documentId, true, data, isDraftEntity);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNotNullReturnsDeleteEvent(String documentId) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, documentId, false, data, false);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNotNullReturnsDoNothingEventIfDraftEvent(String documentId) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, documentId, false, data, true);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@EmptySource
	void documentIdPresentAndExistingNotNullReturnsDoNothingEvent(String documentId) {
		var data = CdsData.create();
		data.put(Attachments.DOCUMENT_ID, "someValue");

		var event = cut.getEvent(null, documentId, true, data, true);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void documentIdPresentAndExistingIdIsNullReturnsNothingToDo(boolean isDraftEntity) {
		var event = cut.getEvent(mock(InputStream.class), "test", true, CdsData.create(), isDraftEntity);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNullReturnsCreateEvent(String documentId) {
		var event = cut.getEvent(mock(InputStream.class), documentId, false, CdsData.create(), true);

		assertThat(event).isEqualTo(createEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void documentIdNotPresentAndExistingNullReturnsDoNothingEvent(String documentId) {
		var event = cut.getEvent(null, documentId, false, CdsData.create(), true);

		assertThat(event).isEqualTo(doNothingEvent);
	}

}

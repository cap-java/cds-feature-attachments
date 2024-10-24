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
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;

class DefaultModifyAttachmentEventFactoryTest {

	private ModifyAttachmentEventFactory cut;
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

		cut = new ModifyAttachmentEventFactory(createEvent, updateEvent, deleteContentEvent, doNothingEvent);
	}

	@Test
	void allNullNothingToDo() {
		var event = cut.getEvent(null, null, true, CdsData.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@Test
	void contentIdsNullContentFilledReturnedCreateEvent() {
		var event = cut.getEvent(mock(InputStream.class), null, true, CdsData.create());

		assertThat(event).isEqualTo(createEvent);
	}

	@Test
	void contentIdNullButtExistingNotNullReturnsDelete() {
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(null, null, true, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@Test
	void contentIdsSameContentFillReturnsUpdate() {
		var contentId = "test ID";
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, contentId);

		var event = cut.getEvent(mock(InputStream.class), contentId, true, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNotNullReturnsUpdateEvent(String contentId) {
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), contentId, false, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@Test
	void contentIdsSameContentNullReturnsNothingToDo() {
		var contentId = "test ID";
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, contentId);

		var event = cut.getEvent(null, contentId, true, data);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNotNullReturnsDeleteEvent(String contentId) {
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(null, contentId, false, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@EmptySource
	void contentIdPresentAndExistingNotNullButDifferentReturnsDeleteEvent(String contentId) {
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(null, contentId, true, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@Test
	void contentIdPresentAndExistingIdIsNullReturnsNothingToDo() {
		var event = cut.getEvent(mock(InputStream.class), "test", true, CdsData.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNullReturnsCreateEvent(String contentId) {
		var event = cut.getEvent(mock(InputStream.class), contentId, false, CdsData.create());

		assertThat(event).isEqualTo(createEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNullReturnsDoNothingEvent(String contentId) {
		var event = cut.getEvent(null, contentId, false, CdsData.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@Test
	void contentIdPresentButNullAndExistingNotNullReturnsUpdateEvent() {
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), null, true, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@Test
	void updateIfContentIdDifferentButContentProvided() {
		var data = CdsData.create();
		data.put(Attachments.CONTENT_ID, "existing");

		var event = cut.getEvent(mock(InputStream.class), "someValue", true, data);

		assertThat(event).isEqualTo(updateEvent);
	}

}

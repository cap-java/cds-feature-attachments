/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;

class ModifyAttachmentEventFactoryTest {

	private ModifyAttachmentEventFactory cut;
	private CreateAttachmentEvent createEvent;
	private UpdateAttachmentEvent updateEvent;
	private MarkAsDeletedAttachmentEvent deleteContentEvent;
	private DoNothingAttachmentEvent doNothingEvent;

	@BeforeEach
	void setup() {
		createEvent = mock(CreateAttachmentEvent.class);
		updateEvent = mock(UpdateAttachmentEvent.class);
		deleteContentEvent = mock(MarkAsDeletedAttachmentEvent.class);
		doNothingEvent = mock(DoNothingAttachmentEvent.class);

		cut = new ModifyAttachmentEventFactory(createEvent, updateEvent, deleteContentEvent, doNothingEvent);
	}

	@Test
	void allNullNothingToDo() {
		var event = cut.getEvent(null, null, Attachments.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@Test
	void contentIdsNullContentFilledReturnedCreateEvent() {
		var event = cut.getEvent(mock(InputStream.class), null, Attachments.create());

		assertThat(event).isEqualTo(createEvent);
	}

	@Test
	void contentIdNullButtExistingNotNullReturnsDelete() {
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(null, null, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@Test
	void contentIdsSameContentFillReturnsUpdate() {
		var contentId = "test ID";
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, contentId);

		var event = cut.getEvent(mock(InputStream.class), contentId, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNotNullReturnsUpdateEvent(String contentId) {
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), contentId, data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@Test
	void contentIdsSameContentNullReturnsNothingToDo() {
		var contentId = "test ID";
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, contentId);

		var event = cut.getEvent(null, contentId, data);

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNotNullReturnsDeleteEvent(String contentId) {
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(null, contentId, data);

		assertThat(event).isEqualTo(deleteContentEvent);
	}

	@Test
	void contentIdPresentAndExistingIdIsNullReturnsNothingToDo() {
		var event = cut.getEvent(mock(InputStream.class), "test", Attachments.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@ParameterizedTest
//	@ValueSource(strings = {"some document Id"})
	@NullSource
//	@EmptySource
	void contentIdNotPresentAndExistingNullReturnsCreateEvent(String contentId) {
		var event = cut.getEvent(mock(InputStream.class), contentId, Attachments.create());

		assertThat(event).isEqualTo(createEvent);
	}

	@ParameterizedTest
	@ValueSource(strings = {"some document Id"})
	@NullSource
	@EmptySource
	void contentIdNotPresentAndExistingNullReturnsDoNothingEvent(String contentId) {
		var event = cut.getEvent(null, contentId, Attachments.create());

		assertThat(event).isEqualTo(doNothingEvent);
	}

	@Test
	void contentIdPresentButNullAndExistingNotNullReturnsUpdateEvent() {
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, "someValue");

		var event = cut.getEvent(mock(InputStream.class), null,  data);

		assertThat(event).isEqualTo(updateEvent);
	}

	@Test
	void updateIfContentIdDifferentButContentProvided() {
		var data = Attachments.create();
		data.put(Attachments.CONTENT_ID, "existing");

		var event = cut.getEvent(mock(InputStream.class), "someValue", data);

		assertThat(event).isEqualTo(updateEvent);
	}

}

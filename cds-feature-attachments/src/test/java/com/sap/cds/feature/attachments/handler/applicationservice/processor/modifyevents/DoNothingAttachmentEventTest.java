package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.services.EventContext;

class DoNothingAttachmentEventTest {
	private DoNothingAttachmentEvent cut;

	@BeforeEach
	void setup() {
		cut = new DoNothingAttachmentEvent();
	}

	@ParameterizedTest
	@ValueSource(strings = {"test content"})
	@NullSource
	@EmptySource
	void contentReturned(String input) {
		var streamInput = Objects.nonNull(input) ? new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)) : null;
		var path = mock(Path.class);
		var element = mock(CdsElement.class);
		var data = mock(CdsData.class);
		var ids = new HashMap<String, Object>();

		var result = cut.processEvent(path, element, streamInput, data, ids, mock(EventContext.class));

		assertThat(result).isEqualTo(streamInput);
		verifyNoInteractions(path, element, data);
		assertThat(ids).isEmpty();
	}

}

package com.sap.cds.feature.attachments.handler.draft;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.services.EventContext;

class DraftHandlerTest {

	private DraftHandler cut;

	@BeforeEach
	void setup() {
		cut = new DraftHandler();
	}

	@Test
	void noExceptionIsThrownInBefore() {
		assertDoesNotThrow(() -> cut.processBefore(mock(EventContext.class), Collections.emptyList()));
	}

	@Test
	void noExceptionIsThrownInAfter() {
		assertDoesNotThrow(() -> cut.processAfter(mock(EventContext.class), Collections.emptyList()));
	}

}

package com.sap.cds.feature.attachments.handler.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.services.cds.CqnService;

class DefaultApplicationEventFactoryTest {

		private DefaultApplicationEventFactory cut;
		private ApplicationEvent createApplicationEvent;
		private ApplicationEvent updateApplicationEvent;
		private ApplicationEvent readApplicationEvent;

		@BeforeEach
		void setup() {
				createApplicationEvent = mock(ApplicationEvent.class);
				updateApplicationEvent = mock(ApplicationEvent.class);
				readApplicationEvent = mock(ApplicationEvent.class);
				cut = new DefaultApplicationEventFactory(createApplicationEvent, updateApplicationEvent, readApplicationEvent);
		}

		@Test
		void createEventReturned() {
				assertThat(cut.getApplicationEvent(CqnService.EVENT_CREATE)).isEqualTo(createApplicationEvent);
		}

		@Test
		void updateEventReturned() {
				assertThat(cut.getApplicationEvent(CqnService.EVENT_UPDATE)).isEqualTo(updateApplicationEvent);
		}

		@Test
		void readEventReturned() {
				assertThat(cut.getApplicationEvent(CqnService.EVENT_READ)).isEqualTo(readApplicationEvent);
		}

		@Test
		void wrongEventThrowsException() {
				assertThrows(IllegalStateException.class, () -> cut.getApplicationEvent("WRONG_EVENT"));
		}

}

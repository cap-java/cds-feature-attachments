package com.sap.cds.feature.attachments.handler.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.handler.AttachmentsHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.persistence.PersistenceService;

class AutoConfigurationTest {

		private AutoConfiguration cut;
		private AttachmentService attachmentService;
		private PersistenceService persistenceService;

		@BeforeEach
		void setup() {
				cut = new AutoConfiguration();

				attachmentService = mock(AttachmentService.class);
				persistenceService = mock(PersistenceService.class);
		}

		@Test
		void eventHandlerBuild() {
				assertThat(cut.buildHandler(persistenceService, attachmentService)).isInstanceOf(AttachmentsHandler.class);
		}

		@Test
		void classHasCorrectAnnotation() {
				assertThat(cut.getClass().getAnnotation(Configuration.class)).isNotNull();
		}

		@Test
		void buildEventHandlerMethodHasCorrectAnnotation() throws NoSuchMethodException {
				var beanAnnotation = cut.getClass().getMethod("buildHandler", PersistenceService.class, AttachmentService.class).getAnnotation(Bean.class);
				assertThat(beanAnnotation).isNotNull();
		}

}

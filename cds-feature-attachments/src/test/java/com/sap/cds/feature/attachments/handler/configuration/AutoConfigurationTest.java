package com.sap.cds.feature.attachments.handler.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.handler.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.UpdateAttachmentsHandler;
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
	void createEventHandlerBuild() {
		assertThat(cut.buildCreateHandler(persistenceService, attachmentService)).isInstanceOf(CreateAttachmentsHandler.class);
	}

	@Test
	void updateEventHandlerBuild() {
		assertThat(cut.buildUpdateHandler(persistenceService, attachmentService)).isInstanceOf(UpdateAttachmentsHandler.class);
	}

	@Test
	void deleteEventHandlerBuild() {
		assertThat(cut.buildDeleteHandler()).isInstanceOf(DeleteAttachmentsHandler.class);
	}

	@Test
	void readEventHandlerBuild() {
		assertThat(cut.buildReadHandler(attachmentService)).isInstanceOf(ReadAttachmentsHandler.class);
	}

	@Test
	void classHasCorrectAnnotation() {
		assertThat(cut.getClass().getAnnotation(Configuration.class)).isNotNull();
	}

}

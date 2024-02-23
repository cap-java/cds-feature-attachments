package com.sap.cds.feature.attachments.handler.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.handler.AttachmentsHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.persistence.PersistenceService;

@Configuration
public class AutoConfiguration {

		@Bean
		public EventHandler buildHandler(PersistenceService persistenceService, AttachmentService attachmentService) {
				return new AttachmentsHandler(persistenceService, attachmentService);
		}

}

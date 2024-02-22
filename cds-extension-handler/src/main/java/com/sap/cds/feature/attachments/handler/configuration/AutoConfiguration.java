package com.sap.cds.feature.attachments.handler.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.handler.AttachmentsHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.handler.EventHandler;

@Configuration
public class AutoConfiguration {

		@Bean
		public EventHandler buildHandler(AttachmentService attachmentService) {
				return new AttachmentsHandler(attachmentService);
		}

}

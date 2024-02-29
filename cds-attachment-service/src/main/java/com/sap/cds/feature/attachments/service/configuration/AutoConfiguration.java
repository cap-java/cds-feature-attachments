package com.sap.cds.feature.attachments.service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.DefaultAttachmentsService;

@Configuration
public class AutoConfiguration {

		@Bean
		public AttachmentService buildAttachmentService() {
				return new DefaultAttachmentsService();
		}

}

package com.sap.cds.feature.attachments.service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.DummyAttachmentsService;

@Configuration
public class AutoConfiguration {

		@Bean
		public AttachmentService buildAttachment() {
				return new DummyAttachmentsService();
		}

}

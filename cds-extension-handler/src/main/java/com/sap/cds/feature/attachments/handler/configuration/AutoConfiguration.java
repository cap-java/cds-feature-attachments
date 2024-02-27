package com.sap.cds.feature.attachments.handler.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.handler.AttachmentsHandler;
import com.sap.cds.feature.attachments.handler.processor.DefaultApplicationEventProcessor;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.CreateApplicationEvent;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.UpdateApplicationEvent;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.DeleteContentAttachmentEvent;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.persistence.PersistenceService;

@Configuration
public class AutoConfiguration {

		@Bean
		public EventHandler buildHandler(PersistenceService persistenceService, AttachmentService attachmentService) {

				var createAttachmentEvent = new CreateAttachmentEvent(attachmentService);
				var updateAttachmentEvent = new UpdateAttachmentEvent(attachmentService);
				var deleteAttachmentEvent = new DeleteContentAttachmentEvent(attachmentService);
				var attachmentEventFactory = new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteAttachmentEvent);
				var createApplicationEvent = new CreateApplicationEvent(persistenceService, attachmentEventFactory);
				var updateApplicationEvent = new UpdateApplicationEvent(persistenceService, attachmentEventFactory);
				var eventProcessor = new DefaultApplicationEventProcessor(createApplicationEvent, updateApplicationEvent);

				return new AttachmentsHandler(eventProcessor);
		}

}

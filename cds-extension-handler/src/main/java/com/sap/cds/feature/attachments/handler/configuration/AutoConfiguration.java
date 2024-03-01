package com.sap.cds.feature.attachments.handler.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cds.feature.attachments.handler.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier.DefaultItemModifierProvider;
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
	public EventHandler buildCreateHandler(PersistenceService persistenceService, AttachmentService attachmentService) {
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService);
		var updateAttachmentEvent = new UpdateAttachmentEvent(attachmentService);
		var deleteAttachmentEvent = new DeleteContentAttachmentEvent(attachmentService);
		var attachmentEventFactory = new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteAttachmentEvent);
		return new CreateAttachmentsHandler(persistenceService, attachmentEventFactory);
	}

	@Bean
	public EventHandler buildDeleteHandler() {
		return new DeleteAttachmentsHandler();
	}

	@Bean
	public EventHandler buildReadHandler(AttachmentService attachmentService) {
		var itemModifierProvider = new DefaultItemModifierProvider();
		return new ReadAttachmentsHandler(attachmentService, itemModifierProvider);
	}

	@Bean
	public EventHandler buildUpdateHandler(PersistenceService persistenceService, AttachmentService attachmentService) {
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService);
		var updateAttachmentEvent = new UpdateAttachmentEvent(attachmentService);
		var deleteAttachmentEvent = new DeleteContentAttachmentEvent(attachmentService);
		var attachmentEventFactory = new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteAttachmentEvent);
		return new UpdateAttachmentsHandler(persistenceService, attachmentEventFactory);
	}

}

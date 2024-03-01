package com.sap.cds.feature.attachments.configuration;

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
import com.sap.cds.feature.attachments.service.DefaultAttachmentsService;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

//TODO JavaDoc
public class Registration implements CdsRuntimeConfiguration {

	@Override
	public void services(CdsRuntimeConfigurer configurer) {
		configurer.service(buildAttachmentService());
	}

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		configurer.eventHandler(new DefaultAttachmentsServiceHandler());

		var persistenceService = configurer.getCdsRuntime().getServiceCatalog().getService(PersistenceService.class, PersistenceService.DEFAULT_NAME);
		var attachmentService = configurer.getCdsRuntime().getServiceCatalog().getService(AttachmentService.class, AttachmentService.DEFAULT_NAME);

		configurer.eventHandler(buildCreateHandler(persistenceService, attachmentService));
		configurer.eventHandler(buildUpdateHandler(persistenceService, attachmentService));
		configurer.eventHandler(buildDeleteHandler());
		configurer.eventHandler(buildReadHandler(attachmentService));
	}

	private AttachmentService buildAttachmentService() {
		return new DefaultAttachmentsService();
	}

	private EventHandler buildCreateHandler(PersistenceService persistenceService, AttachmentService attachmentService) {
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService);
		var updateAttachmentEvent = new UpdateAttachmentEvent(attachmentService);
		var deleteAttachmentEvent = new DeleteContentAttachmentEvent(attachmentService);
		var attachmentEventFactory = new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteAttachmentEvent);
		return new CreateAttachmentsHandler(persistenceService, attachmentEventFactory);
	}

	private EventHandler buildDeleteHandler() {
		return new DeleteAttachmentsHandler();
	}

	private EventHandler buildReadHandler(AttachmentService attachmentService) {
		var itemModifierProvider = new DefaultItemModifierProvider();
		return new ReadAttachmentsHandler(attachmentService, itemModifierProvider);
	}

	private EventHandler buildUpdateHandler(PersistenceService persistenceService, AttachmentService attachmentService) {
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService);
		var updateAttachmentEvent = new UpdateAttachmentEvent(attachmentService);
		var deleteAttachmentEvent = new DeleteContentAttachmentEvent(attachmentService);
		var attachmentEventFactory = new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteAttachmentEvent);
		return new UpdateAttachmentsHandler(persistenceService, attachmentEventFactory);
	}

}

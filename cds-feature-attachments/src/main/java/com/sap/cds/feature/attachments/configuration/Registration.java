package com.sap.cds.feature.attachments.configuration;

import com.sap.cds.feature.attachments.dummy.DummyAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.modifier.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DeleteContentAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.DefaultAssociationCascader;
import com.sap.cds.feature.attachments.handler.common.DefaultAttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.DraftAttachmentsHandler;
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
		configurer.eventHandler(new DummyAttachmentsServiceHandler());

		var persistenceService = configurer.getCdsRuntime().getServiceCatalog().getService(PersistenceService.class, PersistenceService.DEFAULT_NAME);
		var attachmentService = configurer.getCdsRuntime().getServiceCatalog().getService(AttachmentService.class, AttachmentService.DEFAULT_NAME);

		var factory = buildAttachmentEventFactory(attachmentService);
		configurer.eventHandler(buildCreateHandler(persistenceService, factory));
		configurer.eventHandler(buildUpdateHandler(persistenceService, factory));
		configurer.eventHandler(buildDeleteHandler(persistenceService));
		configurer.eventHandler(buildReadHandler(attachmentService));
		configurer.eventHandler(new DraftAttachmentsHandler());
	}

	private AttachmentService buildAttachmentService() {
		return new DefaultAttachmentsService();
	}

	protected DefaultModifyAttachmentEventFactory buildAttachmentEventFactory(AttachmentService attachmentService) {
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService);
		var updateAttachmentEvent = new UpdateAttachmentEvent(attachmentService);
		var deleteAttachmentEvent = new DeleteContentAttachmentEvent(attachmentService);
		var doNothingAttachmentEvent = new DoNothingAttachmentEvent();
		return new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteAttachmentEvent, doNothingAttachmentEvent);
	}

	protected EventHandler buildCreateHandler(PersistenceService persistenceService, ModifyAttachmentEventFactory factory) {
		return new CreateAttachmentsHandler(persistenceService, factory);
	}

	protected EventHandler buildDeleteHandler(PersistenceService persistenceService) {
		var cascader = new DefaultAssociationCascader();
		var attachmentsReader = new DefaultAttachmentsReader(cascader, persistenceService);
		return new DeleteAttachmentsHandler(attachmentsReader);
	}

	protected EventHandler buildReadHandler(AttachmentService attachmentService) {
		return new ReadAttachmentsHandler(attachmentService, BeforeReadItemsModifier::new);
	}

	protected EventHandler buildUpdateHandler(PersistenceService persistenceService, ModifyAttachmentEventFactory factory) {
		return new UpdateAttachmentsHandler(persistenceService, factory);
	}

}

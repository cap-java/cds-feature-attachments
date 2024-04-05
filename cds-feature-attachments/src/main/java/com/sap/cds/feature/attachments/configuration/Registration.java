package com.sap.cds.feature.attachments.configuration;

import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DeleteContentAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.modifier.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.CreationChangeSetListener;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.common.DefaultAssociationCascader;
import com.sap.cds.feature.attachments.handler.common.DefaultAttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.DraftCancelAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftPatchAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.modifier.ActiveEntityModifier;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.DefaultAttachmentsService;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.outbox.OutboxService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;


/**
	* The class {@link Registration} is a configuration class that registers the
	* services and event handlers for the attachments feature.
	*/
public class Registration implements CdsRuntimeConfiguration {

	@Override
	public void services(CdsRuntimeConfigurer configurer) {
		configurer.service(buildAttachmentService());
	}

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		configurer.eventHandler(new DefaultAttachmentsServiceHandler());

		var persistenceService = configurer.getCdsRuntime().getServiceCatalog()
																													.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME);
		var attachmentService = configurer.getCdsRuntime().getServiceCatalog()
																												.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME);
		var outbox = configurer.getCdsRuntime().getServiceCatalog()
																	.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME);
		var outboxedAttachmentService = outbox.outboxed(attachmentService);

		var deleteContentEvent = new DeleteContentAttachmentEvent(outboxedAttachmentService);
		var eventFactory = buildAttachmentEventFactory(attachmentService, deleteContentEvent, outboxedAttachmentService);
		var attachmentsReader = buildAttachmentsReader(persistenceService);

		configurer.eventHandler(buildCreateHandler(eventFactory));
		configurer.eventHandler(buildUpdateHandler(eventFactory, attachmentsReader, outboxedAttachmentService));
		configurer.eventHandler(buildDeleteHandler(attachmentsReader, deleteContentEvent));
		configurer.eventHandler(buildReadHandler(attachmentService));
		configurer.eventHandler(new DraftPatchAttachmentsHandler(persistenceService, eventFactory));
		configurer.eventHandler(new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentEvent, ActiveEntityModifier::new));
	}

	private AttachmentService buildAttachmentService() {
		return new DefaultAttachmentsService();
	}

	protected DefaultModifyAttachmentEventFactory buildAttachmentEventFactory(AttachmentService attachmentService, ModifyAttachmentEvent deleteContentEvent, AttachmentService outboxedAttachmentService) {
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService, outboxedAttachmentService, CreationChangeSetListener::new);
		var updateAttachmentEvent = new UpdateAttachmentEvent(createAttachmentEvent, deleteContentEvent);

		var doNothingAttachmentEvent = new DoNothingAttachmentEvent();
		return new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteContentEvent, doNothingAttachmentEvent);
	}

	protected EventHandler buildCreateHandler(ModifyAttachmentEventFactory factory) {
		return new CreateAttachmentsHandler(factory);
	}

	protected EventHandler buildDeleteHandler(AttachmentsReader attachmentsReader, ModifyAttachmentEvent deleteContentEvent) {
		return new DeleteAttachmentsHandler(attachmentsReader, deleteContentEvent);
	}

	protected EventHandler buildReadHandler(AttachmentService attachmentService) {
		return new ReadAttachmentsHandler(attachmentService, BeforeReadItemsModifier::new);
	}

	protected EventHandler buildUpdateHandler(ModifyAttachmentEventFactory factory, AttachmentsReader attachmentsReader, AttachmentService outboxedAttachmentService) {
		return new UpdateAttachmentsHandler(factory, attachmentsReader, outboxedAttachmentService);
	}

	protected AttachmentsReader buildAttachmentsReader(PersistenceService persistenceService) {
		var cascader = new DefaultAssociationCascader();
		return new DefaultAttachmentsReader(cascader, persistenceService);
	}

}

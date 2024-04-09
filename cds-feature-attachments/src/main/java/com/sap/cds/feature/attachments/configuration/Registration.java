package com.sap.cds.feature.attachments.configuration;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.MarkAsDeletedAttachmentEvent;
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
import com.sap.cds.feature.attachments.service.AttachmentMalwareScanService;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.DefaultAttachmentMalwareScanService;
import com.sap.cds.feature.attachments.service.DefaultAttachmentsService;
import com.sap.cds.feature.attachments.service.client.DefaultMalwareScanClient;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentMalwareScanServiceHandler;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.utilities.LoggingMarker;
import com.sap.cds.services.environment.CdsProperties.ConnectionPool;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.outbox.OutboxService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.environment.ServiceBindingUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;


/**
	* The class {@link Registration} is a configuration class that registers the
	* services and event handlers for the attachments feature.
	*/
public class Registration implements CdsRuntimeConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Registration.class);
	private static final Marker marker = LoggingMarker.ATTACHMENT_SERVICE_REGISTRATION.getMarker();

	@Override
	public void services(CdsRuntimeConfigurer configurer) {
		configurer.service(buildAttachmentService());
		configurer.service(buildMalwareScanService());
	}

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		logger.info(marker, "Registering event handler for attachment service");

		var malwareScanService = configurer.getCdsRuntime().getServiceCatalog()
																													.getService(AttachmentMalwareScanService.class, AttachmentMalwareScanService.DEFAULT_NAME);
		var persistenceService = configurer.getCdsRuntime().getServiceCatalog()
																													.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME);
		var attachmentService = configurer.getCdsRuntime().getServiceCatalog()
																												.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME);
		var outbox = configurer.getCdsRuntime().getServiceCatalog()
																	.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME);
		var outboxedAttachmentService = outbox.outboxed(attachmentService);
		var outboxedMalwareScanService = outbox.outboxed(malwareScanService);

		configurer.eventHandler(new DefaultAttachmentsServiceHandler(outboxedMalwareScanService));

		List<ServiceBinding> bindings = configurer.getCdsRuntime().getEnvironment().getServiceBindings()
																																				.filter(b -> ServiceBindingUtils.matches(b, DefaultMalwareScanClient.NAME_MALWARE_SCANNER))
																																				.toList();
		var binding = !bindings.isEmpty() ? bindings.get(0) : null;
		var connectionPoll = new ConnectionPool(Duration.ofSeconds(60), 2, 20);
		configurer.eventHandler(new DefaultAttachmentMalwareScanServiceHandler(persistenceService, attachmentService, new DefaultMalwareScanClient(binding, configurer.getCdsRuntime(), connectionPoll)));

		var deleteContentEvent = new MarkAsDeletedAttachmentEvent(outboxedAttachmentService);
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
		logger.info(marker, "Registering attachment service");
		return new DefaultAttachmentsService();
	}

	private AttachmentMalwareScanService buildMalwareScanService() {
		logger.info(marker, "Registering malware scan service");
		return new DefaultAttachmentMalwareScanService();
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

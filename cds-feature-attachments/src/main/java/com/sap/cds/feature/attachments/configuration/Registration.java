/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadLocalDataStorage;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.modifier.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator.DefaultAttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.CreationChangeSetListener;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.common.DefaultAssociationCascader;
import com.sap.cds.feature.attachments.handler.common.DefaultAttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.DraftActiveAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftCancelAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftPatchAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.modifier.ActiveEntityModifier;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.AttachmentsServiceImpl;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanRunner;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.feature.attachments.service.malware.DefaultAttachmentMalwareScanner;
import com.sap.cds.feature.attachments.service.malware.client.DefaultMalwareScanClient;
import com.sap.cds.feature.attachments.service.malware.client.httpclient.MalwareScanClientProviderFactory;
import com.sap.cds.feature.attachments.service.malware.client.mapper.DefaultMalwareClientStatusMapper;
import com.sap.cds.feature.attachments.service.malware.constants.MalwareScanConstants;
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
	}

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		logger.info(marker, "Registering event handler for attachment service");

		var persistenceService = configurer.getCdsRuntime().getServiceCatalog().getService(PersistenceService.class,
				PersistenceService.DEFAULT_NAME);
		var attachmentService = configurer.getCdsRuntime().getServiceCatalog().getService(AttachmentService.class,
				AttachmentService.DEFAULT_NAME);
		var outbox = configurer.getCdsRuntime().getServiceCatalog().getService(OutboxService.class,
				OutboxService.PERSISTENT_UNORDERED_NAME);
		var outboxedAttachmentService = outbox.outboxed(attachmentService);

		List<ServiceBinding> bindings = configurer.getCdsRuntime().getEnvironment().getServiceBindings().filter(
				b -> ServiceBindingUtils.matches(b, MalwareScanConstants.MALWARE_SCAN_SERVICE_LABEL)).toList();
		var binding = !bindings.isEmpty() ? bindings.get(0) : null;
		var connectionPoll = new ConnectionPool(Duration.ofSeconds(60), 2, 20);
		var clientProviderFactory = new MalwareScanClientProviderFactory(binding, configurer.getCdsRuntime(), connectionPoll);
		var malwareStatusMapper = new DefaultMalwareClientStatusMapper();
		var malwareScanner = new DefaultAttachmentMalwareScanner(persistenceService, attachmentService,
				new DefaultMalwareScanClient(clientProviderFactory), malwareStatusMapper, Objects.nonNull(binding));
		var malwareScanEndTransactionListener = createEndTransactionMalwareScanListener(malwareScanner);
		configurer.eventHandler(new DefaultAttachmentsServiceHandler(malwareScanEndTransactionListener));

		var deleteContentEvent = new MarkAsDeletedAttachmentEvent(outboxedAttachmentService);
		var eventFactory = buildAttachmentEventFactory(attachmentService, deleteContentEvent, outboxedAttachmentService);
		var attachmentsReader = buildAttachmentsReader(persistenceService);
		ThreadLocalDataStorage storage = new ThreadLocalDataStorage();

		configurer.eventHandler(buildCreateHandler(eventFactory, storage));
		configurer.eventHandler(buildUpdateHandler(eventFactory, attachmentsReader, outboxedAttachmentService, storage));
		configurer.eventHandler(buildDeleteHandler(attachmentsReader, deleteContentEvent));
		configurer.eventHandler(
				buildReadHandler(attachmentService, new EndTransactionMalwareScanRunner(null, null, malwareScanner)));
		configurer.eventHandler(new DraftPatchAttachmentsHandler(persistenceService, eventFactory));
		configurer.eventHandler(
				new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentEvent, ActiveEntityModifier::new));
		configurer.eventHandler(new DraftActiveAttachmentsHandler(storage));
	}

	private EndTransactionMalwareScanProvider createEndTransactionMalwareScanListener(
			DefaultAttachmentMalwareScanner malwareScanner) {
		return (attachmentEntity, contentId) -> new EndTransactionMalwareScanRunner(attachmentEntity, contentId,
				malwareScanner);
	}

	private AttachmentService buildAttachmentService() {
		logger.info(marker, "Registering AttachmentService");
		return new AttachmentsServiceImpl();
	}

	protected DefaultModifyAttachmentEventFactory buildAttachmentEventFactory(AttachmentService attachmentService,
			ModifyAttachmentEvent deleteContentEvent, AttachmentService outboxedAttachmentService) {
		var creationChangeSetListener = createCreationFailedListener(outboxedAttachmentService);
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService, creationChangeSetListener);
		var updateAttachmentEvent = new UpdateAttachmentEvent(createAttachmentEvent, deleteContentEvent);

		var doNothingAttachmentEvent = new DoNothingAttachmentEvent();
		return new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteContentEvent,
				doNothingAttachmentEvent);
	}

	private ListenerProvider createCreationFailedListener(AttachmentService outboxedAttachmentService) {
		return (contentId, cdsRuntime) -> new CreationChangeSetListener(contentId, cdsRuntime, outboxedAttachmentService);
	}

	protected EventHandler buildCreateHandler(ModifyAttachmentEventFactory factory, ThreadLocalDataStorage storage) {
		return new CreateAttachmentsHandler(factory, storage);
	}

	protected EventHandler buildDeleteHandler(AttachmentsReader attachmentsReader,
			ModifyAttachmentEvent deleteContentEvent) {
		return new DeleteAttachmentsHandler(attachmentsReader, deleteContentEvent);
	}

	protected EventHandler buildReadHandler(AttachmentService attachmentService,
			AsyncMalwareScanExecutor asyncMalwareScanExecutor) {
		var statusValidator = new DefaultAttachmentStatusValidator();
		return new ReadAttachmentsHandler(attachmentService, BeforeReadItemsModifier::new, statusValidator,
				asyncMalwareScanExecutor);
	}

	protected EventHandler buildUpdateHandler(ModifyAttachmentEventFactory factory, AttachmentsReader attachmentsReader,
			AttachmentService outboxedAttachmentService, ThreadLocalDataStorage storage) {
		return new UpdateAttachmentsHandler(factory, attachmentsReader, outboxedAttachmentService, storage);
	}

	protected AttachmentsReader buildAttachmentsReader(PersistenceService persistenceService) {
		var cascader = new DefaultAssociationCascader();
		return new DefaultAttachmentsReader(cascader, persistenceService);
	}

}

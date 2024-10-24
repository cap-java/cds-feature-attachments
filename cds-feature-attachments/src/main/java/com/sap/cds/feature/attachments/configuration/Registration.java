/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadLocalDataStorage;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.CreationChangeSetListener;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction.ListenerProvider;
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
import com.sap.cds.feature.attachments.service.malware.DefaultAttachmentMalwareScanner;
import com.sap.cds.feature.attachments.service.malware.client.DefaultMalwareScanClient;
import com.sap.cds.feature.attachments.service.malware.client.httpclient.MalwareScanClientProviderFactory;
import com.sap.cds.feature.attachments.service.malware.client.mapper.MalwareClientStatusMapper;
import com.sap.cds.feature.attachments.service.malware.constants.MalwareScanConstants;
import com.sap.cds.services.ServiceCatalog;
import com.sap.cds.services.environment.CdsProperties.ConnectionPool;
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

	@Override
	public void services(CdsRuntimeConfigurer configurer) {
		configurer.service(new AttachmentsServiceImpl());
	}

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		logger.info("Registering event handler for attachment service");

		ServiceCatalog catalog = configurer.getCdsRuntime().getServiceCatalog();
		var persistenceService = catalog.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME);
		var attachmentService = catalog.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME);
		var outbox = catalog.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME);
		var outboxedAttachmentService = outbox.outboxed(attachmentService);

		List<ServiceBinding> bindings = configurer.getCdsRuntime().getEnvironment().getServiceBindings().filter(
				b -> ServiceBindingUtils.matches(b, MalwareScanConstants.MALWARE_SCAN_SERVICE_LABEL)).toList();
		var binding = !bindings.isEmpty() ? bindings.get(0) : null;
		var connectionPoll = new ConnectionPool(Duration.ofSeconds(60), 2, 20);
		var clientProviderFactory = new MalwareScanClientProviderFactory(binding, configurer.getCdsRuntime(), connectionPoll);
		var malwareStatusMapper = new MalwareClientStatusMapper();
		var malwareScanner = new DefaultAttachmentMalwareScanner(persistenceService, attachmentService,
				new DefaultMalwareScanClient(clientProviderFactory), malwareStatusMapper, Objects.nonNull(binding));
		var malwareScanEndTransactionListener = createEndTransactionMalwareScanListener(malwareScanner, configurer);
		configurer.eventHandler(new DefaultAttachmentsServiceHandler(malwareScanEndTransactionListener));

		var deleteContentEvent = new MarkAsDeletedAttachmentEvent(outboxedAttachmentService);
		var eventFactory = buildAttachmentEventFactory(attachmentService, deleteContentEvent, outboxedAttachmentService);
		var attachmentsReader = new DefaultAttachmentsReader(new DefaultAssociationCascader(), persistenceService);
		ThreadLocalDataStorage storage = new ThreadLocalDataStorage();

		// register event handlers on the AttachmentsService
		configurer.eventHandler(new CreateAttachmentsHandler(eventFactory, storage));
		configurer.eventHandler(
				new UpdateAttachmentsHandler(eventFactory, attachmentsReader, outboxedAttachmentService, storage));
		configurer.eventHandler(new DeleteAttachmentsHandler(attachmentsReader, deleteContentEvent));
		var malwareScanRunner = new EndTransactionMalwareScanRunner(null, null, malwareScanner,
				configurer.getCdsRuntime());
		configurer.eventHandler(new ReadAttachmentsHandler(attachmentService, new AttachmentStatusValidator(),
				malwareScanRunner));

		// register event handlers on the DraftService
		configurer.eventHandler(new DraftPatchAttachmentsHandler(persistenceService, eventFactory));
		configurer.eventHandler(
				new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentEvent, ActiveEntityModifier::new));
		configurer.eventHandler(new DraftActiveAttachmentsHandler(storage));
	}

	private EndTransactionMalwareScanProvider createEndTransactionMalwareScanListener(
			DefaultAttachmentMalwareScanner malwareScanner, CdsRuntimeConfigurer configurer) {
		return (attachmentEntity, contentId) -> new EndTransactionMalwareScanRunner(attachmentEntity, contentId,
				malwareScanner, configurer.getCdsRuntime());
	}

	private ModifyAttachmentEventFactory buildAttachmentEventFactory(AttachmentService attachmentService,
			ModifyAttachmentEvent deleteContentEvent, AttachmentService outboxedAttachmentService) {
		ListenerProvider creationChangeSetListener = (contentId, cdsRuntime) -> new CreationChangeSetListener(contentId, cdsRuntime, outboxedAttachmentService);
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService, creationChangeSetListener);
		var updateAttachmentEvent = new UpdateAttachmentEvent(createAttachmentEvent, deleteContentEvent);

		var doNothingAttachmentEvent = new DoNothingAttachmentEvent();
		return new ModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteContentEvent,
				doNothingAttachmentEvent);
	}

}

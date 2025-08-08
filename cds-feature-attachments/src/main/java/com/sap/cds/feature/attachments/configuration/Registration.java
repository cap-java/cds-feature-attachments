/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.configuration;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadLocalDataStorage;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.CreateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.CreationChangeSetListener;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.DraftActiveAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftCancelAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftPatchAttachmentsHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.AttachmentsServiceImpl;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanRunner;
import com.sap.cds.feature.attachments.service.malware.AttachmentMalwareScanner;
import com.sap.cds.feature.attachments.service.malware.DefaultAttachmentMalwareScanner;
import com.sap.cds.feature.attachments.service.malware.client.DefaultMalwareScanClient;
import com.sap.cds.feature.attachments.service.malware.client.HttpClientProvider;
import com.sap.cds.feature.attachments.service.malware.client.MalwareScanClient;
import com.sap.cds.feature.attachments.service.malware.client.MalwareScanClientProvider;
import com.sap.cds.services.ServiceCatalog;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties.ConnectionPool;
import com.sap.cds.services.outbox.OutboxService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.environment.ServiceBindingUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * The class {@link Registration} is a configuration class that registers the services and event handlers for the
 * attachments feature.
 */
public class Registration implements CdsRuntimeConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Registration.class);

	@Override
	public void services(CdsRuntimeConfigurer configurer) {
		configurer.service(new AttachmentsServiceImpl());
	}

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		logger.debug("Registering event handlers");

		CdsRuntime runtime = configurer.getCdsRuntime();
		ServiceCatalog serviceCatalog = runtime.getServiceCatalog();

		// get required services from the service catalog
		PersistenceService persistenceService = serviceCatalog.getService(PersistenceService.class,
				PersistenceService.DEFAULT_NAME);
		AttachmentService attachmentService = serviceCatalog.getService(AttachmentService.class,
				AttachmentService.DEFAULT_NAME);

		// outbox AttachmentService if OutboxService is available
		OutboxService outboxService = serviceCatalog.getService(OutboxService.class,
				OutboxService.PERSISTENT_UNORDERED_NAME);
		AttachmentService outboxedAttachmentService;
		if (outboxService != null) {
			outboxedAttachmentService = outboxService.outboxed(attachmentService);
		} else {
			outboxedAttachmentService = attachmentService;
			logger.warn("OutboxService '{}' is not available. AttachmentService will not be outboxed.",
					OutboxService.PERSISTENT_UNORDERED_NAME);
		}

		// build malware scanner client, could be null if no service binding is available
		MalwareScanClient scanClient = buildMalwareScanClient(runtime.getEnvironment());

		AttachmentMalwareScanner malwareScanner = new DefaultAttachmentMalwareScanner(persistenceService,
				attachmentService, scanClient);

		EndTransactionMalwareScanProvider malwareScanEndTransactionListener = (attachmentEntity,
				contentId) -> new EndTransactionMalwareScanRunner(attachmentEntity, contentId, malwareScanner, runtime);

		// register event handlers for attachment service
		configurer.eventHandler(new DefaultAttachmentsServiceHandler(malwareScanEndTransactionListener));

		MarkAsDeletedAttachmentEvent deleteEvent = new MarkAsDeletedAttachmentEvent(outboxedAttachmentService);
		ModifyAttachmentEventFactory eventFactory = buildAttachmentEventFactory(attachmentService, deleteEvent,
				outboxedAttachmentService);
		AttachmentsReader attachmentsReader = new AttachmentsReader(new AssociationCascader(), persistenceService);
		ThreadLocalDataStorage storage = new ThreadLocalDataStorage();

		// register event handlers for application service, if at least one application service is available
		boolean hasApplicationServices = serviceCatalog.getServices(ApplicationService.class).findFirst().isPresent();
		if (hasApplicationServices) {
			configurer.eventHandler(new CreateAttachmentsHandler(eventFactory, storage));
			configurer.eventHandler(
					new UpdateAttachmentsHandler(eventFactory, attachmentsReader, outboxedAttachmentService, storage));
			configurer.eventHandler(new DeleteAttachmentsHandler(attachmentsReader, deleteEvent));
			EndTransactionMalwareScanRunner scanRunner = new EndTransactionMalwareScanRunner(null, null, malwareScanner,
					runtime);
			configurer.eventHandler(
					new ReadAttachmentsHandler(attachmentService, new AttachmentStatusValidator(), scanRunner));
		} else {
			logger.debug(
					"No application service is available. Application service event handlers will not be registered.");
		}

		// register event handlers on draft service, if at least one draft service is available
		boolean hasDraftServices = serviceCatalog.getServices(DraftService.class).findFirst().isPresent();
		if (hasDraftServices) {
			configurer.eventHandler(new DraftPatchAttachmentsHandler(persistenceService, eventFactory));
			configurer.eventHandler(new DraftCancelAttachmentsHandler(attachmentsReader, deleteEvent));
			configurer.eventHandler(new DraftActiveAttachmentsHandler(storage));
		} else {
			logger.debug("No draft service is available. Draft event handlers will not be registered.");
		}
	}

	/**
	 * Builds the {@link MalwareScanClient malware scanner client} based on the service binding.
	 *
	 * @param environment the {@link CdsEnvironment environment} to retrieve the service binding from
	 * @return the {@link MalwareScanClient malware scanner client} or {@code null} if no service binding is available
	 */
	private static MalwareScanClient buildMalwareScanClient(CdsEnvironment environment) {
		// retrieve the service binding for the malware scanner service
		Optional<ServiceBinding> bindingOpt = environment.getServiceBindings()
				.filter(b -> ServiceBindingUtils.matches(b, DefaultAttachmentMalwareScanner.MALWARE_SCAN_SERVICE_LABEL))
				.findFirst();

		if (bindingOpt.isPresent()) {
			ServiceBinding binding = bindingOpt.get();
			ConnectionPool connectionPool = getConnectionPool(environment);
			HttpClientProvider clientProvider = new MalwareScanClientProvider(binding, connectionPool);
			if (logger.isInfoEnabled()) {
				logger.info(
						"Using Malware Scanning service binding with name '{}' and plan '{}' for malware scanning of attachments.",
						binding.getName().orElse("unknown"), binding.getServicePlan().orElse("unknown"));
			}
			return new DefaultMalwareScanClient(clientProvider);
		}

		logger.info("No Malware Scanning service binding found, malware scanning is disabled.");
		return null;
	}

	private static ModifyAttachmentEventFactory buildAttachmentEventFactory(AttachmentService attachmentService,
			MarkAsDeletedAttachmentEvent deleteEvent, AttachmentService outboxedAttachmentService) {
		ListenerProvider listenerProvider = (contentId, cdsRuntime) -> new CreationChangeSetListener(contentId,
				cdsRuntime, outboxedAttachmentService);
		CreateAttachmentEvent createEvent = new CreateAttachmentEvent(attachmentService, listenerProvider);
		UpdateAttachmentEvent updateEvent = new UpdateAttachmentEvent(createEvent, deleteEvent);
		DoNothingAttachmentEvent doNothingEvent = new DoNothingAttachmentEvent();
		return new ModifyAttachmentEventFactory(createEvent, updateEvent, deleteEvent, doNothingEvent);
	}

	private static ConnectionPool getConnectionPool(CdsEnvironment env) {
		// the common prefix for the connection pool configuration
		final String prefix = "cds.attachments.malwareScanner.http.%s";
		Duration timeout = Duration.ofSeconds(env.getProperty(prefix.formatted("timeout"), Integer.class, 120));
		int maxConnections = env.getProperty(prefix.formatted("maxConnections"), Integer.class, 20);
		logger.debug("Connection pool configuration: timeout={}, maxConnections={}", timeout, maxConnections);
		return new ConnectionPool(timeout, maxConnections, maxConnections);
	}
}

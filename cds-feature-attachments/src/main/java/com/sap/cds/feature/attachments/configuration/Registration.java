/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
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
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DoNothingAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.UpdateAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator.DefaultAttachmentStatusValidator;
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
import com.sap.cds.feature.attachments.service.malware.client.mapper.DefaultMalwareClientStatusMapper;
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
		logger.debug("Registering event handlers");

		CdsRuntime runtime = configurer.getCdsRuntime();
		ServiceCatalog serviceCatalog = runtime.getServiceCatalog();
		CdsEnvironment environment = runtime.getEnvironment();

		// get required services from the service catalog
		PersistenceService persistenceService = serviceCatalog.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME);
		AttachmentService attachmentService = serviceCatalog.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME);

		// outbox AttachmentService if OutboxService is available
		OutboxService outboxService = serviceCatalog.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME);
		AttachmentService outboxedAttachmentService;
		if (outboxService != null) {
			outboxedAttachmentService = outboxService.outboxed(attachmentService);
		} else {
			outboxedAttachmentService = attachmentService;
			logger.warn("OutboxService '{}' is not available. AttachmentService will not be outboxed.",
					OutboxService.PERSISTENT_UNORDERED_NAME);
		}

		// retrieve the service binding for the malware scanner service
		List<ServiceBinding> bindings = environment.getServiceBindings()
				.filter(b -> ServiceBindingUtils.matches(b, DefaultAttachmentMalwareScanner.MALWARE_SCAN_SERVICE_LABEL)).toList();
		var binding = !bindings.isEmpty() ? bindings.get(0) : null;

		// get HTTP connection pool configuration
		var connectionPool = getConnectionPool(environment);
		var clientProviderFactory = new MalwareScanClientProviderFactory(binding, connectionPool);
		var malwareStatusMapper = new DefaultMalwareClientStatusMapper();
		var malwareScanner = new DefaultAttachmentMalwareScanner(persistenceService, attachmentService,
				new DefaultMalwareScanClient(clientProviderFactory), malwareStatusMapper, Objects.nonNull(binding));
		EndTransactionMalwareScanProvider malwareScanEndTransactionListener = (attachmentEntity,
				contentId) -> new EndTransactionMalwareScanRunner(attachmentEntity, contentId, malwareScanner, runtime);

		// register event handlers for attachment service
		configurer.eventHandler(new DefaultAttachmentsServiceHandler(malwareScanEndTransactionListener));

		var deleteContentEvent = new MarkAsDeletedAttachmentEvent(outboxedAttachmentService);
		var eventFactory = buildAttachmentEventFactory(attachmentService, deleteContentEvent, outboxedAttachmentService);
		var attachmentsReader = new DefaultAttachmentsReader(new DefaultAssociationCascader(), persistenceService);
		ThreadLocalDataStorage storage = new ThreadLocalDataStorage();

		// register event handlers for application service, if at least one application service is available
		boolean hasApplicationServices = serviceCatalog.getServices(ApplicationService.class).findFirst().isPresent();
		if (hasApplicationServices) {
			configurer.eventHandler(new CreateAttachmentsHandler(eventFactory, storage));
			configurer.eventHandler(new UpdateAttachmentsHandler(eventFactory, attachmentsReader, outboxedAttachmentService, storage));
			configurer.eventHandler(new DeleteAttachmentsHandler(attachmentsReader, deleteContentEvent));
			var scanRunner = new EndTransactionMalwareScanRunner(null, null, malwareScanner, runtime);
			configurer.eventHandler(new ReadAttachmentsHandler(attachmentService, new DefaultAttachmentStatusValidator(), scanRunner));
		} else {
			logger.debug("No application service is available. Application service event handlers will not be registered.");
		}

		// register event handlers on draft service, if at least one draft service is available
		boolean hasDraftServices = serviceCatalog.getServices(DraftService.class).findFirst().isPresent();
		if (hasDraftServices) {
			configurer.eventHandler(new DraftPatchAttachmentsHandler(persistenceService, eventFactory));
			configurer.eventHandler(new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentEvent,
					ActiveEntityModifier::new));
			configurer.eventHandler(new DraftActiveAttachmentsHandler(storage));
		} else {
			logger.debug("No draft service is available. Draft event handlers will not be registered.");
		}
	}

	private DefaultModifyAttachmentEventFactory buildAttachmentEventFactory(AttachmentService attachmentService,
			ModifyAttachmentEvent deleteContentEvent, AttachmentService outboxedAttachmentService) {
		ListenerProvider creationChangeSetListener = (contentId, cdsRuntime) -> new CreationChangeSetListener(contentId, cdsRuntime, outboxedAttachmentService);
		var createAttachmentEvent = new CreateAttachmentEvent(attachmentService, creationChangeSetListener);
		var updateAttachmentEvent = new UpdateAttachmentEvent(createAttachmentEvent, deleteContentEvent);

		var doNothingAttachmentEvent = new DoNothingAttachmentEvent();
		return new DefaultModifyAttachmentEventFactory(createAttachmentEvent, updateAttachmentEvent, deleteContentEvent,
				doNothingAttachmentEvent);
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

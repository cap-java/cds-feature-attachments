/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.handler.applicationservice.ApplicationServiceAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftActiveAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftCancelAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.draftservice.DraftPatchAttachmentsHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.malware.DefaultAttachmentMalwareScanner;
import com.sap.cds.services.Service;
import com.sap.cds.services.ServiceCatalog;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.outbox.OutboxService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrationTest {

  private Registration cut;
  private CdsRuntimeConfigurer configurer;
  private ServiceCatalog serviceCatalog;
  private PersistenceService persistenceService;
  private AttachmentService attachmentService;
  private OutboxService outboxService;
  private DraftService draftService;
  private ApplicationService applicationService;
  private ArgumentCaptor<Service> serviceArgumentCaptor;
  private ArgumentCaptor<EventHandler> handlerArgumentCaptor;

  @BeforeEach
  void setup() {
    cut = new Registration();

    configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    when(configurer.getCdsRuntime()).thenReturn(cdsRuntime);
    serviceCatalog = mock(ServiceCatalog.class);
    when(cdsRuntime.getServiceCatalog()).thenReturn(serviceCatalog);
    CdsEnvironment environment = mock(CdsEnvironment.class);
    when(environment.getProperty(any(), any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(2));
    ServiceBinding binding = mock(ServiceBinding.class);
    when(binding.getServiceName())
        .thenReturn(Optional.of(DefaultAttachmentMalwareScanner.MALWARE_SCAN_SERVICE_LABEL));
    when(binding.getName()).thenReturn(Optional.of("malware-scanner"));
    when(binding.getServicePlan()).thenReturn(Optional.of("clamav"));
    when(environment.getServiceBindings()).thenReturn(Stream.of(binding));
    when(cdsRuntime.getEnvironment()).thenReturn(environment);
    persistenceService = mock(PersistenceService.class);
    attachmentService = mock(AttachmentService.class);
    outboxService = mock(OutboxService.class);
    doReturn(attachmentService).when(outboxService).outboxed(any(AttachmentService.class));
    draftService = mock(DraftService.class);
    applicationService = mock(ApplicationService.class);
    serviceArgumentCaptor = ArgumentCaptor.forClass(Service.class);
    handlerArgumentCaptor = ArgumentCaptor.forClass(EventHandler.class);
  }

  @Test
  void serviceIsRegistered() {
    cut.services(configurer);

    verify(configurer).service(serviceArgumentCaptor.capture());
    var services = serviceArgumentCaptor.getAllValues();
    assertThat(services).hasSize(1);

    var attachmentServiceFound = services.stream().anyMatch(AttachmentService.class::isInstance);

    assertThat(attachmentServiceFound).isTrue();
  }

  @Test
  void handlersAreRegistered() {
    when(serviceCatalog.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME))
        .thenReturn(persistenceService);
    when(serviceCatalog.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME))
        .thenReturn(attachmentService);
    when(serviceCatalog.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME))
        .thenReturn(outboxService);
    when(serviceCatalog.getServices(DraftService.class)).thenReturn(Stream.of(draftService));
    when(serviceCatalog.getServices(ApplicationService.class))
        .thenReturn(Stream.of(applicationService));

    cut.eventHandlers(configurer);

    var handlerSize = 5;
    verify(configurer, times(handlerSize)).eventHandler(handlerArgumentCaptor.capture());
    checkHandlers(handlerArgumentCaptor.getAllValues(), handlerSize);
  }

  @Test
  void handlersAreRegisteredWithoutOutboxService() {
    when(serviceCatalog.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME))
        .thenReturn(persistenceService);
    when(serviceCatalog.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME))
        .thenReturn(attachmentService);
    when(serviceCatalog.getServices(DraftService.class)).thenReturn(Stream.of(draftService));
    when(serviceCatalog.getServices(ApplicationService.class))
        .thenReturn(Stream.of(applicationService));
    // Return null for OutboxService to test the missing branch
    when(serviceCatalog.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME))
        .thenReturn(null);

    cut.eventHandlers(configurer);

    var handlerSize = 5;
    verify(configurer, times(handlerSize)).eventHandler(handlerArgumentCaptor.capture());
    checkHandlers(handlerArgumentCaptor.getAllValues(), handlerSize);
  }

  private void checkHandlers(List<EventHandler> handlers, int handlerSize) {
    assertThat(handlers).hasSize(handlerSize);
    isHandlerForClassIncluded(handlers, DefaultAttachmentsServiceHandler.class);
    isHandlerForClassIncluded(handlers, ApplicationServiceAttachmentsHandler.class);
    isHandlerForClassIncluded(handlers, DraftPatchAttachmentsHandler.class);
    isHandlerForClassIncluded(handlers, DraftCancelAttachmentsHandler.class);
    isHandlerForClassIncluded(handlers, DraftActiveAttachmentsHandler.class);
  }

  @Test
  void lessHandlersAreRegistered() {
    when(serviceCatalog.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME))
        .thenReturn(persistenceService);
    when(serviceCatalog.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME))
        .thenReturn(attachmentService);
    when(serviceCatalog.getService(OutboxService.class, OutboxService.PERSISTENT_UNORDERED_NAME))
        .thenReturn(outboxService);

    cut.eventHandlers(configurer);

    var handlerSize = 1;
    verify(configurer, times(handlerSize)).eventHandler(handlerArgumentCaptor.capture());
    var handlers = handlerArgumentCaptor.getAllValues();
    assertThat(handlers).hasSize(handlerSize);
    isHandlerForClassIncluded(handlers, DefaultAttachmentsServiceHandler.class);
    // event handlers for application services are not registered
    isHandlerForClassMissing(handlers, ApplicationServiceAttachmentsHandler.class);
    // event handlers for draft services are not registered
    isHandlerForClassMissing(handlers, DraftPatchAttachmentsHandler.class);
    isHandlerForClassMissing(handlers, DraftCancelAttachmentsHandler.class);
    isHandlerForClassMissing(handlers, DraftActiveAttachmentsHandler.class);
  }

  private void isHandlerForClassIncluded(
      List<EventHandler> handlers, Class<? extends EventHandler> includedClass) {
    var isHandlerIncluded =
        handlers.stream().anyMatch(handler -> handler.getClass() == includedClass);
    assertThat(isHandlerIncluded).isTrue();
  }

  private void isHandlerForClassMissing(
      List<EventHandler> handlers, Class<? extends EventHandler> includedClass) {
    var isHandlerIncluded =
        handlers.stream().anyMatch(handler -> handler.getClass() == includedClass);
    assertThat(isHandlerIncluded).isFalse();
  }
}

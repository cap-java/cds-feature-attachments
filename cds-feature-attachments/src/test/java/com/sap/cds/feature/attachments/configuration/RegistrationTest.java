package com.sap.cds.feature.attachments.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.handler.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.DeleteAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.handler.DefaultAttachmentsServiceHandler;
import com.sap.cds.services.Service;
import com.sap.cds.services.ServiceCatalog;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

class RegistrationTest {

		private Registration cut;
		private CdsRuntimeConfigurer configurer;
		private ServiceCatalog serviceCatalog;
		private PersistenceService persistenceService;
		private AttachmentService attachmentService;
		private ArgumentCaptor<Service> serviceArgumentCaptor;
		private ArgumentCaptor<EventHandler> handlerArgumentCaptor;

		private static void isHandlerForClassIncluded(List<EventHandler> handlers, Class<? extends EventHandler> includedClass) {
				var isHandlerIncluded = handlers.stream().anyMatch(handler -> handler.getClass() == includedClass);
				assertThat(isHandlerIncluded).isTrue();
		}

		@BeforeEach
		void setup() {
				cut = new Registration();

				configurer = mock(CdsRuntimeConfigurer.class);
				CdsRuntime cdsRuntime = mock(CdsRuntime.class);
				when(configurer.getCdsRuntime()).thenReturn(cdsRuntime);
				serviceCatalog = mock(ServiceCatalog.class);
				when(cdsRuntime.getServiceCatalog()).thenReturn(serviceCatalog);
				persistenceService = mock(PersistenceService.class);
				attachmentService = mock(AttachmentService.class);
				serviceArgumentCaptor = ArgumentCaptor.forClass(Service.class);
				handlerArgumentCaptor = ArgumentCaptor.forClass(EventHandler.class);
		}

		@Test
		void serviceIsRegistered() {
				cut.services(configurer);

				verify(configurer).service(serviceArgumentCaptor.capture());
				var service = serviceArgumentCaptor.getValue();
				assertThat(service).isInstanceOf(AttachmentService.class);
		}

		@Test
		void handlersAreRegistered() {
				when(serviceCatalog.getService(PersistenceService.class, PersistenceService.DEFAULT_NAME)).thenReturn(persistenceService);
				when(serviceCatalog.getService(AttachmentService.class, AttachmentService.DEFAULT_NAME)).thenReturn(attachmentService);

				cut.eventHandlers(configurer);

				verify(configurer, times(5)).eventHandler(handlerArgumentCaptor.capture());
				var handlers = handlerArgumentCaptor.getAllValues();
				assertThat(handlers).hasSize(5);
				isHandlerForClassIncluded(handlers, DefaultAttachmentsServiceHandler.class);
				isHandlerForClassIncluded(handlers, CreateAttachmentsHandler.class);
				isHandlerForClassIncluded(handlers, UpdateAttachmentsHandler.class);
				isHandlerForClassIncluded(handlers, DeleteAttachmentsHandler.class);
				isHandlerForClassIncluded(handlers, ReadAttachmentsHandler.class);
		}

}

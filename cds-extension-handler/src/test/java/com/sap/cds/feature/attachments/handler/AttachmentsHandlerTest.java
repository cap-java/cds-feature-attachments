package com.sap.cds.feature.attachments.handler;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.runtime.CdsRuntime;

class AttachmentsHandlerTest {

		private AttachmentsHandler cut;
		private AttachmentService service;
		private CdsRuntime runtime;

		@BeforeEach
		void setup() {
				service = mock(AttachmentService.class);
				cut = new AttachmentsHandler(service);

				runtime = prepareRuntime();
		}

		@Test
		void simpleUpdateDoesNotCallAttachment() {
//				cut.
		}

		private CdsRuntime prepareRuntime() {
//				var runtime = CdsRuntimeConfigurer.create()
//						.cdsModel("cds/gen/src/main/resources/edmx/csn.json")
//						.serviceConfigurations()
//						.eventHandlerConfigurations()
//						.complete();
//
//				runtime.getServiceCatalog().getServices(ApplicationLifecycleService.class).forEach(ApplicationLifecycleService::applicationPrepared);
//				return runtime;
				return null;
		}

}

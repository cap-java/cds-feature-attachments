package com.sap.cds.feature.attachments.handler;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

abstract class AttachmentsHandlerTestBase {

		public static final String CSN_FILE_PATH = "cds/gen/src/main/resources/edmx/csn.json";
		CdsRuntime runtime;

		void setup() {
				runtime = prepareRuntime();
		}

		private CdsRuntime prepareRuntime() {
				var runtime = CdsRuntimeConfigurer.create()
						.cdsModel(CSN_FILE_PATH)
						.serviceConfigurations()
						.eventHandlerConfigurations()
						.complete();
				runtime.getServiceCatalog().getServices(ApplicationLifecycleService.class).forEach(ApplicationLifecycleService::applicationPrepared);
				return runtime;
		}

}

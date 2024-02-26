package com.sap.cds.feature.attachments.handler.helper;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

public class RuntimeHelper {

		public static final String CSN_FILE_PATH = "cds/gen/src/main/resources/edmx/csn.json";
		public final CdsRuntime runtime;

		public RuntimeHelper() {
				this.runtime = prepareRuntime();
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

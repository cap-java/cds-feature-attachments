package com.sap.cds.feature.attachments.handler.helper;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

public class RuntimeHelper {

	private static final String CSN_FILE_PATH = "gen/src/main/resources/edmx/csn.json";
	public static final CdsRuntime runtime = prepareRuntime();

	private static CdsRuntime prepareRuntime() {
		var runtime = CdsRuntimeConfigurer.create().cdsModel(CSN_FILE_PATH).serviceConfigurations()
				.eventHandlerConfigurations().complete();
		runtime.getServiceCatalog().getServices(ApplicationLifecycleService.class)
				.forEach(ApplicationLifecycleService::applicationPrepared);
		return runtime;
	}

	private RuntimeHelper() {
		// avoid instantiation
	}
}

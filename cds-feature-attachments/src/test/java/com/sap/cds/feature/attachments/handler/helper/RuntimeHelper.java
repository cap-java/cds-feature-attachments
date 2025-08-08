/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.helper;

import org.mockito.Mockito;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

public class RuntimeHelper {

	private static final String CSN_FILE_PATH = "gen/src/main/resources/edmx/csn.json";
	public static final CdsRuntime runtime = prepareRuntime();

	private static CdsRuntime prepareRuntime() {
		PersistenceService persistenceService = Mockito.mock(PersistenceService.class);
		Mockito.when(persistenceService.getName()).thenReturn(PersistenceService.DEFAULT_NAME);

		CdsRuntime runtime = CdsRuntimeConfigurer.create().cdsModel(CSN_FILE_PATH).service(persistenceService)
				.serviceConfigurations().eventHandlerConfigurations().complete();
		runtime.getServiceCatalog().getServices(ApplicationLifecycleService.class)
				.forEach(ApplicationLifecycleService::applicationPrepared);
		return runtime;
	}

	private RuntimeHelper() {
		// avoid instantiation
	}
}

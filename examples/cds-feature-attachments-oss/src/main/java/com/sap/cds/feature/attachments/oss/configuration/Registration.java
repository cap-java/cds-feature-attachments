package com.sap.cds.feature.attachments.oss.configuration;

import com.sap.cds.OSService;
import com.sap.cds.OSServiceImpl;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

/**
 * The class registers the event handlers for the attachments feature based on filesystem.
 */
public class Registration implements CdsRuntimeConfiguration {

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		OSService osService = new OSServiceImpl();

		configurer.eventHandler(new OSSAttachmentsServiceHandler(osService));
	}
}
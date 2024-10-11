package com.sap.cds.feature.attachments.fs.configuration;

import java.io.IOException;

import com.sap.cds.feature.attachments.fs.handler.FSAttachmentsServiceHandler;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

/**
 * The class {@link Registration} is a configuration class that registers the services and event handlers for the
 * attachments feature based on filesystem.
 */
public class Registration implements CdsRuntimeConfiguration {

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		try {
			configurer.eventHandler(new FSAttachmentsServiceHandler());
		} catch (IOException e) {
			throw new IllegalStateException("Error while creating the FSAttachmentsServiceHandler", e);
		}
	}

}
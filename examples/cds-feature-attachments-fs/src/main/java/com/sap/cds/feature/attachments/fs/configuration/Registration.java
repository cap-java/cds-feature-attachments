/*
 * © 2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.fs.configuration;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import com.sap.cds.feature.attachments.fs.handler.FSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.fs.handler.FSDraftServiceHandler;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

/**
 * The class registers the event handlers for the attachments feature based on filesystem.
 */
public class Registration implements CdsRuntimeConfiguration {

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		Path tmpPath = FileUtils.getTempDirectory().toPath();
		Path rootFolder = tmpPath.resolve("com.sap.cds.cds-feature-attachments-fs");

		try {
			configurer.eventHandler(new FSAttachmentsServiceHandler(rootFolder));
			configurer.eventHandler(new FSDraftServiceHandler());
		} catch (IOException e) {
			throw new IllegalStateException("Error while creating the FSAttachmentsServiceHandler", e);
		}
	}

}
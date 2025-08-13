package com.sap.cds.feature.attachments.oss.configuration;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * The class registers the event handlers for the attachments feature based on filesystem.
 */
public class Registration implements CdsRuntimeConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(Registration.class);

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		Optional<ServiceBinding> binding = getOSBinding(configurer.getCdsRuntime().getEnvironment());
		configurer.eventHandler(new OSSAttachmentsServiceHandler(binding));
		logger.info("Registered OSS Attachments Service Handler.");
	}

	/**
	 * Builds the {@link OSService object store service} based on the service binding.
	 *
	 * @param environment the {@link CdsEnvironment environment} to retrieve the service binding from
	 * @return the {@link OSService object store service} or {@code null} if no service binding is available
	 */
	public static Optional<ServiceBinding> getOSBinding(CdsEnvironment environment) {
		Optional<ServiceBinding> bindingOpt = environment.getServiceBindings()
			.filter(b -> b.getServiceName().map(name -> name.equals("objectstore")).orElse(false))
			.findFirst();
		if (!bindingOpt.isPresent()) {
			logger.info("No objectstore binding found when registering the attachments plugin.");
		}
		return bindingOpt;
	}
}

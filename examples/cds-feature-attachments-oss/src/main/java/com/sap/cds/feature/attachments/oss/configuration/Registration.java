package com.sap.cds.feature.attachments.oss.configuration;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.oss.client.MockServiceBinding;
import com.sap.cds.feature.attachments.oss.client.OSService;
import com.sap.cds.feature.attachments.oss.client.OSServiceImpl;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cds.services.utils.environment.ServiceBindingUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * The class registers the event handlers for the attachments feature based on filesystem.
 */
public class Registration implements CdsRuntimeConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(Registration.class);

	@Override
	public void eventHandlers(CdsRuntimeConfigurer configurer) {
		OSService osService = buildOSService(configurer.getCdsRuntime().getEnvironment());
		configurer.eventHandler(new OSSAttachmentsServiceHandler(osService));
	}

	/**
	 * Builds the {@link OSService object store service} based on the service binding.
	 *
	 * @param environment the {@link CdsEnvironment environment} to retrieve the service binding from
	 * @return the {@link OSService object store service} or {@code null} if no service binding is available
	 */
	public static OSService buildOSService(CdsEnvironment environment) {
		// retrieve the service binding for the malware scanner service
		Optional<ServiceBinding> bindingOpt = environment.getServiceBindings()
				.filter(b -> ServiceBindingUtils.matches(b, "object-store-attachments"))
				.findFirst();
		ServiceBinding binding;
		if (bindingOpt.isPresent()) {
			binding = bindingOpt.get();
		} else {
			logger.info("bindingOpt not there.");
			// Mock the ServiceBinding interface
			binding = new MockServiceBinding();
		}

		return new OSServiceImpl(binding);
	}
}
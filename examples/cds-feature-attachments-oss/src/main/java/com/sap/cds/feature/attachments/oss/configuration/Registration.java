package com.sap.cds.feature.attachments.oss.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.oss.client.MockServiceBinding;
import com.sap.cds.feature.attachments.oss.client.OSService;
import com.sap.cds.feature.attachments.oss.client.OSServiceImpl;
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
		List<ServiceBinding> allBindings = environment.getServiceBindings()
			.collect(Collectors.toList());

		if (!allBindings.isEmpty()) {
			System.out.println("***************************In Registration of CDS-FEATURE-ATTACHMENTS-OSS: " + allBindings.size());
			ServiceBinding firstBinding = allBindings.get(0);
			System.out.println("Name: " + firstBinding.getName());
			System.out.println("ServiceName: " + firstBinding.getServiceName());
			Map<String, Object> credentials = firstBinding.getCredentials();
			System.out.println("Credentials size: " + credentials.size());
			System.out.println("Credentials: " + credentials);
			List<String> tags = firstBinding.getTags();
			System.err.println("Tags: " + tags);
		}
		// Filter afterward
		Optional<ServiceBinding> bindingOpt = allBindings.stream()
		.filter(b -> b.getServiceName().map(name -> name.equals("objectstore")).orElse(false))
		.findFirst();
		ServiceBinding binding;
		if (bindingOpt.isPresent()) {
			binding = bindingOpt.get();
			Map<String, Object> credentials = binding.getCredentials();
			System.out.println("***************************In Registration: " + credentials.size());
		} else {
			System.out.println("***************************In Registration: no binding found");
			logger.info("bindingOpt not there.");
			binding = new MockServiceBinding();
		}
		return new OSServiceImpl(binding);
	}
}
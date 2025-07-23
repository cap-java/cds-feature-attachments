package com.sap.cds.feature.attachments.oss.configuration;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.oss.client.OSService;
import com.sap.cds.feature.attachments.oss.client.OSServiceImpl;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.environment.CdsProperties.ConnectionPool;
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

		if (bindingOpt.isPresent()) {
			ServiceBinding binding = bindingOpt.get();
			ConnectionPool connectionPool = getConnectionPool(environment);
			if (logger.isInfoEnabled()) {
				logger.info("Using Object Store Service binding: " + binding.getName());
			}
			return new OSServiceImpl(binding, connectionPool);
		}

		logger.info("No OS Service enabled, using HANA to store large objects.");
		return null;
	}
	private static ConnectionPool getConnectionPool(CdsEnvironment env) {
		// the common prefix for the connection pool configuration
		// todo: change this to the correct prefix
		final String prefix = "cds.attachments.objectStore.http.%s";
		Duration timeout = Duration.ofSeconds(env.getProperty(prefix.formatted("timeout"), Integer.class, 120));
		int maxConnections = env.getProperty(prefix.formatted("maxConnections"), Integer.class, 20);
		logger.debug("Connection pool configuration: timeout={}, maxConnections={}", timeout, maxConnections);
		return new ConnectionPool(timeout, maxConnections, maxConnections);
	}
}
/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.configuration;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The class registers the event handlers for the attachments feature based on filesystem. */
public class Registration implements CdsRuntimeConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(Registration.class);

  @Override
  public void eventHandlers(CdsRuntimeConfigurer configurer) {
    Optional<ServiceBinding> binding = getOSBinding(configurer.getCdsRuntime().getEnvironment());
    ExecutorService executor =
        Executors
            .newCachedThreadPool(); // This might be configured by CdsProperties, if needed in the
    // future.
    configurer.eventHandler(new OSSAttachmentsServiceHandler(binding, executor));
    logger.info("Registered OSS Attachments Service Handler.");
  }

  /**
   * Retrieves the {@link ServiceBinding} for the object store service from the given {@link
   * CdsEnvironment}.
   *
   * @param environment the {@link CdsEnvironment} to retrieve the service binding from
   * @return an {@link Optional} containing the {@link ServiceBinding} for "objectstore" if
   *     available, or {@link Optional#empty()} if not found
   */
  private static Optional<ServiceBinding> getOSBinding(CdsEnvironment environment) {
    Optional<ServiceBinding> bindingOpt =
        environment
            .getServiceBindings()
            .filter(b -> b.getServiceName().map(name -> name.equals("objectstore")).orElse(false))
            .findFirst();
    return bindingOpt;
  }
}

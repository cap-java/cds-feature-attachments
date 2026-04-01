/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.configuration;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.TenantCleanupHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The class registers the event handlers for the attachments feature based on object store. */
public class Registration implements CdsRuntimeConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(Registration.class);

  @Override
  public void eventHandlers(CdsRuntimeConfigurer configurer) {
    CdsEnvironment env = configurer.getCdsRuntime().getEnvironment();
    Optional<ServiceBinding> bindingOpt = getOSBinding(env);
    if (bindingOpt.isPresent()) {
      boolean multitenancyEnabled = isMultitenancyEnabled(env);
      String objectStoreKind = getObjectStoreKind(env);

      ExecutorService executor = Executors.newCachedThreadPool();
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    executor.shutdown();
                    try {
                      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                      }
                    } catch (InterruptedException e) {
                      executor.shutdownNow();
                      Thread.currentThread().interrupt();
                    }
                  }));
      OSSAttachmentsServiceHandler handler =
          new OSSAttachmentsServiceHandler(
              bindingOpt.get(), executor, multitenancyEnabled, objectStoreKind);
      configurer.eventHandler(handler);

      if (multitenancyEnabled && "shared".equals(objectStoreKind)) {
        configurer.eventHandler(new TenantCleanupHandler(handler.getOsClient()));
        logger.info(
            "Registered OSS Attachments Service Handler with shared multitenancy mode and tenant cleanup.");
      } else {
        logger.info("Registered OSS Attachments Service Handler.");
      }
    } else {
      logger.warn(
          "No service binding to Object Store Service found, hence the OSS Attachments Service Handler is not connected!");
    }
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
    return environment
        .getServiceBindings()
        .filter(b -> b.getServiceName().map(name -> name.equals("objectstore")).orElse(false))
        .findFirst();
  }

  private static boolean isMultitenancyEnabled(CdsEnvironment env) {
    return Boolean.TRUE.equals(
        env.getProperty("cds.multitenancy.enabled", Boolean.class, Boolean.FALSE));
  }

  private static String getObjectStoreKind(CdsEnvironment env) {
    return env.getProperty("cds.attachments.objectStore.kind", String.class, null);
  }
}

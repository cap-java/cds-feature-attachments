/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.configuration;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.client.OSClientFactory;
import com.sap.cds.feature.attachments.oss.client.SharedOSClientProvider;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.TenantCleanupHandler;
import com.sap.cds.feature.attachments.oss.multitenancy.SeparateOSClientProvider;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerCredentials;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerTokenProvider;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.net.http.HttpClient;
import java.time.Duration;
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
    boolean multitenancyEnabled = isMultitenancyEnabled(env);
    String objectStoreKind = getObjectStoreKind(env);

    if (multitenancyEnabled && "separate".equals(objectStoreKind)) {
      registerSeparateMode(configurer, env);
    } else {
      registerSharedOrSingleTenantMode(configurer, env, multitenancyEnabled, objectStoreKind);
    }
  }

  private void registerSeparateMode(CdsRuntimeConfigurer configurer, CdsEnvironment env) {
    Optional<ServiceBinding> smBindingOpt = getServiceManagerBinding(env);
    if (smBindingOpt.isEmpty()) {
      logger.error(
          "Separate-bucket multitenancy requires a 'service-manager' service binding, but none was"
              + " found. OSS Attachments will not be registered.");
      return;
    }

    ExecutorService executor = createExecutor(env);
    ServiceManagerCredentials smCreds =
        ServiceManagerCredentials.fromServiceBinding(smBindingOpt.get());
    HttpClient httpClient = HttpClient.newHttpClient();
    ServiceManagerTokenProvider tokenProvider =
        new ServiceManagerTokenProvider(smCreds, httpClient);
    ServiceManagerClient smClient = new ServiceManagerClient(smCreds, tokenProvider, httpClient);

    int ttlHours =
        env.getProperty(
            "cds.attachments.objectStore.separate.credentialTtlHours", Integer.class, 11);
    SeparateOSClientProvider clientProvider =
        new SeparateOSClientProvider(smClient, executor, Duration.ofHours(ttlHours));

    OSSAttachmentsServiceHandler handler =
        new OSSAttachmentsServiceHandler(clientProvider, true, "separate");
    configurer.eventHandler(handler);

    logger.info(
        "Registered OSS Attachments Service Handler with separate-bucket multitenancy mode."
            + " Tenant lifecycle (onboarding/offboarding) is managed by the cap-js MTX sidecar.");
  }

  private void registerSharedOrSingleTenantMode(
      CdsRuntimeConfigurer configurer,
      CdsEnvironment env,
      boolean multitenancyEnabled,
      String objectStoreKind) {
    Optional<ServiceBinding> bindingOpt = getOSBinding(env);
    if (bindingOpt.isEmpty()) {
      logger.warn(
          "No service binding to Object Store Service found, hence the OSS Attachments Service"
              + " Handler is not connected!");
      return;
    }

    ExecutorService executor = createExecutor(env);
    OSClient osClient = OSClientFactory.create(bindingOpt.get(), executor);
    var osClientProvider = new SharedOSClientProvider(osClient);
    OSSAttachmentsServiceHandler handler =
        new OSSAttachmentsServiceHandler(osClientProvider, multitenancyEnabled, objectStoreKind);
    configurer.eventHandler(handler);

    if (multitenancyEnabled && "shared".equals(objectStoreKind)) {
      configurer.eventHandler(new TenantCleanupHandler(osClient));
      logger.info(
          "Registered OSS Attachments Service Handler with shared multitenancy mode and tenant"
              + " cleanup.");
    } else {
      logger.info("Registered OSS Attachments Service Handler.");
    }
  }

  private ExecutorService createExecutor(CdsEnvironment env) {
    int threadPoolSize =
        env.getProperty("cds.attachments.objectStore.threadPoolSize", Integer.class, 16);
    ExecutorService executor =
        Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
              Thread t = new Thread(r, "attachment-oss-tasks");
              t.setDaemon(true);
              return t;
            });
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
    return executor;
  }

  private static Optional<ServiceBinding> getOSBinding(CdsEnvironment environment) {
    return environment
        .getServiceBindings()
        .filter(b -> b.getServiceName().map(name -> name.equals("objectstore")).orElse(false))
        .findFirst();
  }

  private static Optional<ServiceBinding> getServiceManagerBinding(CdsEnvironment environment) {
    return environment
        .getServiceBindings()
        .filter(b -> b.getServiceName().map(name -> name.equals("service-manager")).orElse(false))
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

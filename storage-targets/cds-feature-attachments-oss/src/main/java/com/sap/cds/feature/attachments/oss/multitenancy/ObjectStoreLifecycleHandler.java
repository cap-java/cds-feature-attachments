/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.client.OSClientFactory;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerBinding;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient.ServiceManagerBindingResult;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerException;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Business logic for tenant lifecycle management in separate-bucket multitenancy mode. Handles
 * provisioning (subscribe) and deprovisioning (unsubscribe) of per-tenant object store buckets via
 * Service Manager.
 */
public class ObjectStoreLifecycleHandler {

  private static final Logger logger = LoggerFactory.getLogger(ObjectStoreLifecycleHandler.class);

  private final ServiceManagerClient smClient;
  private final SeparateOSClientProvider clientProvider;
  private final ExecutorService executor;

  public ObjectStoreLifecycleHandler(
      ServiceManagerClient smClient,
      SeparateOSClientProvider clientProvider,
      ExecutorService executor) {
    this.smClient = smClient;
    this.clientProvider = clientProvider;
    this.executor = executor;
  }

  /**
   * Provisions an object store bucket for a new tenant. Idempotent — if a binding already exists,
   * the cache is simply warmed.
   */
  public void onTenantSubscribe(String tenantId) {
    logger.info("Provisioning object store for tenant {}", tenantId);

    // Idempotency: check if binding already exists
    var existingBinding = smClient.getBinding(tenantId);
    if (existingBinding.isPresent()) {
      logger.info("Binding already exists for tenant {}, warming cache", tenantId);
      warmCache(tenantId, existingBinding.get());
      return;
    }

    String offeringId = smClient.getOfferingId();
    String planId = smClient.getPlanId(offeringId);

    String instanceId = smClient.createInstance(tenantId, planId);
    try {
      ServiceManagerBindingResult binding = smClient.createBinding(tenantId, instanceId);
      warmCache(tenantId, binding);
      logger.info("Provisioned object store for tenant {}: instance={}", tenantId, instanceId);
    } catch (ServiceManagerException e) {
      // Binding creation failed — clean up orphaned instance
      logger.error(
          "Failed to create binding for tenant {}, cleaning up instance {}", tenantId, instanceId);
      try {
        smClient.deleteInstance(instanceId);
      } catch (ServiceManagerException cleanupEx) {
        logger.error("Failed to clean up orphaned instance {}", instanceId, cleanupEx);
      }
      throw e;
    }
  }

  /**
   * Deprovisions the object store for a tenant. Deletes all objects in the bucket, then removes the
   * SM binding and instance. Errors are logged but do not block the unsubscribe flow.
   */
  public void onTenantUnsubscribe(String tenantId) {
    logger.info("Deprovisioning object store for tenant {}", tenantId);

    var bindingResult = smClient.getBinding(tenantId);
    if (bindingResult.isEmpty()) {
      logger.warn("No binding found for tenant {} during unsubscribe, nothing to clean up", tenantId);
      clientProvider.evict(tenantId);
      return;
    }

    var binding = bindingResult.get();

    // Delete all objects in the tenant's bucket
    try {
      OSClient client =
          OSClientFactory.create(new ServiceManagerBinding(binding.credentials()), executor);
      client.deleteContentByPrefix("").get();
      logger.info("Deleted all objects for tenant {}", tenantId);
    } catch (Exception e) {
      logger.error("Failed to delete objects for tenant {}", tenantId, e);
    }

    clientProvider.evict(tenantId);

    // Delete SM binding
    try {
      smClient.deleteBinding(binding.bindingId());
    } catch (ServiceManagerException e) {
      logger.error("Failed to delete SM binding for tenant {}", tenantId, e);
    }

    // Delete SM instance
    try {
      smClient.deleteInstance(binding.instanceId());
    } catch (ServiceManagerException e) {
      logger.error("Failed to delete SM instance for tenant {}", tenantId, e);
    }

    logger.info("Deprovisioned object store for tenant {}", tenantId);
  }

  private void warmCache(String tenantId, ServiceManagerBindingResult binding) {
    OSClient client =
        OSClientFactory.create(new ServiceManagerBinding(binding.credentials()), executor);
    clientProvider.put(tenantId, client);
  }
}

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.oss.client.TenantOSClientProvider;
import com.sap.cds.feature.attachments.oss.servicemanager.ServiceManagerClient;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.DeploymentService;
import com.sap.cds.services.mt.SubscribeEventContext;
import com.sap.cds.services.mt.UnsubscribeEventContext;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeploymentService handler that provisions and deprovisions per-tenant object store instances via
 * the SAP Service Manager. Used in separate-bucket multitenancy mode.
 *
 * <p>On subscribe, creates an object store instance and binding for the tenant. On unsubscribe,
 * deletes the binding and instance and evicts the cached client.
 *
 * <p>All operations are best-effort: errors are logged but not rethrown, so tenant
 * subscribe/unsubscribe is not blocked by object store lifecycle failures.
 */
@ServiceName(DeploymentService.DEFAULT_NAME)
public class ObjectStoreInstanceLifecycleHandler implements EventHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(ObjectStoreInstanceLifecycleHandler.class);

  private final ServiceManagerClient serviceManagerClient;
  private final TenantOSClientProvider tenantOSClientProvider;

  public ObjectStoreInstanceLifecycleHandler(
      ServiceManagerClient serviceManagerClient, TenantOSClientProvider tenantOSClientProvider) {
    this.serviceManagerClient =
        requireNonNull(serviceManagerClient, "serviceManagerClient must not be null");
    this.tenantOSClientProvider =
        requireNonNull(tenantOSClientProvider, "tenantOSClientProvider must not be null");
  }

  @After(event = DeploymentService.EVENT_SUBSCRIBE)
  void provisionObjectStore(SubscribeEventContext context) {
    String tenantId = context.getTenant();
    try {
      OSSAttachmentsServiceHandler.validateTenantId(tenantId);

      if (serviceManagerClient.bindingExistsForTenant(tenantId)) {
        logger.info(
            "Object store binding already exists for tenant {}. Skipping provisioning.", tenantId);
        return;
      }

      String offeringId = serviceManagerClient.getOfferingId();
      String planId = serviceManagerClient.getPlanId(offeringId);
      String instanceId = serviceManagerClient.createInstance(tenantId, planId);
      serviceManagerClient.createBinding(tenantId, instanceId);

      logger.info("Object store instance and binding provisioned for tenant {}", tenantId);
    } catch (Exception e) {
      logger.error("Failed to provision object store for tenant {}", tenantId, e);
    }
  }

  @After(event = DeploymentService.EVENT_UNSUBSCRIBE)
  void deprovisionObjectStore(UnsubscribeEventContext context) {
    String tenantId = context.getTenant();
    try {
      OSSAttachmentsServiceHandler.validateTenantId(tenantId);

      tenantOSClientProvider.evict(tenantId);

      Optional<String> bindingId = serviceManagerClient.findBindingIdForTenant(tenantId);
      if (bindingId.isPresent()) {
        serviceManagerClient.deleteBinding(bindingId.get());
      } else {
        logger.warn("No object store binding found for tenant {} during deprovisioning", tenantId);
      }

      Optional<String> instanceId = serviceManagerClient.findInstanceIdForTenant(tenantId);
      if (instanceId.isPresent()) {
        serviceManagerClient.deleteInstance(instanceId.get());
      } else {
        logger.warn("No object store instance found for tenant {} during deprovisioning", tenantId);
      }

      logger.info("Object store deprovisioned for tenant {}", tenantId);
    } catch (Exception e) {
      logger.error("Failed to deprovision object store for tenant {}", tenantId, e);
    }
  }
}

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.client.TenantOSClientProvider;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.DeploymentService;
import com.sap.cds.services.mt.UnsubscribeEventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler that cleans up a tenant's attachment resources when the tenant unsubscribes.
 *
 * <p>In <b>shared</b> multitenancy mode, deletes all objects with the tenant's prefix from the
 * shared object store.
 *
 * <p>In <b>separate</b> multitenancy mode, evicts the cached {@link OSClient} for the tenant. The
 * actual deletion of the object store instance is handled by the MTX sidecar (cap-js/attachments
 * plugin).
 */
@ServiceName(DeploymentService.DEFAULT_NAME)
public class TenantCleanupHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(TenantCleanupHandler.class);
  private final OSClient osClient;
  private final TenantOSClientProvider tenantOSClientProvider;
  private final boolean separateMode;

  /** Creates a handler for shared multitenancy mode (prefix-based cleanup). */
  public TenantCleanupHandler(OSClient osClient) {
    this.osClient = osClient;
    this.tenantOSClientProvider = null;
    this.separateMode = false;
  }

  /** Creates a handler for separate multitenancy mode (cache eviction only). */
  public TenantCleanupHandler(TenantOSClientProvider tenantOSClientProvider) {
    this.osClient = null;
    this.tenantOSClientProvider = tenantOSClientProvider;
    this.separateMode = true;
  }

  @After(event = DeploymentService.EVENT_UNSUBSCRIBE)
  void cleanupTenantData(UnsubscribeEventContext context) {
    String tenantId = context.getTenant();
    OSSAttachmentsServiceHandler.validateTenantId(tenantId);

    if (separateMode) {
      cleanupSeparateMode(tenantId);
    } else {
      cleanupSharedMode(tenantId);
    }
  }

  private void cleanupSharedMode(String tenantId) {
    String prefix = tenantId + "/";
    try {
      osClient.deleteContentByPrefix(prefix).get();
      logger.info("Cleaned up all objects for tenant {} from shared object store", tenantId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Interrupted while cleaning up objects for tenant {}", tenantId, e);
    } catch (Exception e) {
      logger.error("Failed to clean up objects for tenant {}", tenantId, e);
    }
  }

  private void cleanupSeparateMode(String tenantId) {
    tenantOSClientProvider.evict(tenantId);
    logger.info(
        "Evicted cached OSClient for tenant {} (instance cleanup handled by MTX sidecar)",
        tenantId);
  }
}

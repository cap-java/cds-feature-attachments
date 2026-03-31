/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.DeploymentService;
import com.sap.cds.services.mt.UnsubscribeEventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceName(DeploymentService.DEFAULT_NAME)
public class TenantCleanupHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(TenantCleanupHandler.class);
  private final OSClient osClient;

  public TenantCleanupHandler(OSClient osClient) {
    this.osClient = osClient;
  }

  @After(event = DeploymentService.EVENT_UNSUBSCRIBE)
  void cleanupTenantData(UnsubscribeEventContext context) {
    String tenantId = context.getTenant();
    OSSAttachmentsServiceHandler.validateTenantId(tenantId);
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
}

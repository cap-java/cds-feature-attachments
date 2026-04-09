/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.DeploymentService;
import com.sap.cds.services.mt.UnsubscribeEventContext;

/**
 * CAP event handler that deprovisions a per-tenant object store bucket on tenant unsubscription.
 */
@ServiceName(DeploymentService.DEFAULT_NAME)
public class ObjectStoreUnsubscribeHandler implements EventHandler {

  private final ObjectStoreLifecycleHandler lifecycleHandler;

  public ObjectStoreUnsubscribeHandler(ObjectStoreLifecycleHandler lifecycleHandler) {
    this.lifecycleHandler = lifecycleHandler;
  }

  @After(event = DeploymentService.EVENT_UNSUBSCRIBE)
  void onUnsubscribe(UnsubscribeEventContext context) {
    lifecycleHandler.onTenantUnsubscribe(context.getTenant());
  }
}

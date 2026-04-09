/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.mt.DeploymentService;
import com.sap.cds.services.mt.SubscribeEventContext;

/** CAP event handler that provisions a per-tenant object store bucket on tenant subscription. */
@ServiceName(DeploymentService.DEFAULT_NAME)
public class ObjectStoreSubscribeHandler implements EventHandler {

  private final ObjectStoreLifecycleHandler lifecycleHandler;

  public ObjectStoreSubscribeHandler(ObjectStoreLifecycleHandler lifecycleHandler) {
    this.lifecycleHandler = lifecycleHandler;
  }

  @After(event = DeploymentService.EVENT_SUBSCRIBE)
  void onSubscribe(SubscribeEventContext context) {
    lifecycleHandler.onTenantSubscribe(context.getTenant());
  }
}

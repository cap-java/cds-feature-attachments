/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.services.mt.UnsubscribeEventContext;
import org.junit.jupiter.api.Test;

class ObjectStoreUnsubscribeHandlerTest {

  @Test
  void testOnUnsubscribeDelegatesToLifecycleHandler() {
    ObjectStoreLifecycleHandler lifecycle = mock(ObjectStoreLifecycleHandler.class);
    ObjectStoreUnsubscribeHandler handler = new ObjectStoreUnsubscribeHandler(lifecycle);

    UnsubscribeEventContext context = mock(UnsubscribeEventContext.class);
    when(context.getTenant()).thenReturn("tenant-1");

    handler.onUnsubscribe(context);

    verify(lifecycle).onTenantUnsubscribe("tenant-1");
  }
}

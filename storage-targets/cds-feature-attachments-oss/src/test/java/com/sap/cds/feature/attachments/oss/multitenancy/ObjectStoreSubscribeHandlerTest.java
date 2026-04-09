/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.services.mt.SubscribeEventContext;
import org.junit.jupiter.api.Test;

class ObjectStoreSubscribeHandlerTest {

  @Test
  void testOnSubscribeDelegatesToLifecycleHandler() {
    ObjectStoreLifecycleHandler lifecycle = mock(ObjectStoreLifecycleHandler.class);
    ObjectStoreSubscribeHandler handler = new ObjectStoreSubscribeHandler(lifecycle);

    SubscribeEventContext context = mock(SubscribeEventContext.class);
    when(context.getTenant()).thenReturn("tenant-1");

    handler.onSubscribe(context);

    verify(lifecycle).onTenantSubscribe("tenant-1");
  }
}

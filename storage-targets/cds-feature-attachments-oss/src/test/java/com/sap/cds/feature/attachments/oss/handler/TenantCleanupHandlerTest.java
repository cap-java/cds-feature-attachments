/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.services.mt.UnsubscribeEventContext;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class TenantCleanupHandlerTest {

  @Test
  void testCleanupTenantDataCallsDeleteByPrefix() throws Exception {
    OSClient mockOsClient = mock(OSClient.class);
    UnsubscribeEventContext context = mock(UnsubscribeEventContext.class);
    when(context.getTenant()).thenReturn("tenant1");
    when(mockOsClient.deleteContentByPrefix("tenant1/"))
        .thenReturn(CompletableFuture.completedFuture(null));

    TenantCleanupHandler handler = new TenantCleanupHandler(mockOsClient);
    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant1/");
  }

  @Test
  void testCleanupTenantDataHandlesInterruptedException() throws Exception {
    OSClient mockOsClient = mock(OSClient.class);
    UnsubscribeEventContext context = mock(UnsubscribeEventContext.class);
    when(context.getTenant()).thenReturn("tenant2");

    @SuppressWarnings("unchecked")
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.deleteContentByPrefix("tenant2/")).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    TenantCleanupHandler handler = new TenantCleanupHandler(mockOsClient);
    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant2/");
  }

  @Test
  void testCleanupTenantDataHandlesRuntimeException() throws Exception {
    OSClient mockOsClient = mock(OSClient.class);
    UnsubscribeEventContext context = mock(UnsubscribeEventContext.class);
    when(context.getTenant()).thenReturn("tenant3");

    when(mockOsClient.deleteContentByPrefix("tenant3/"))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

    TenantCleanupHandler handler = new TenantCleanupHandler(mockOsClient);
    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant3/");
  }
}

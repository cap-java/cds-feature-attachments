/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.mt.UnsubscribeEventContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantCleanupHandlerTest {

  private OSClient mockOsClient;
  private TenantCleanupHandler handler;

  @BeforeEach
  void setup() {
    mockOsClient = mock(OSClient.class);
    handler = new TenantCleanupHandler(mockOsClient);
  }

  @Test
  void testCleanupTenantDataCallsDeleteByPrefix() throws Exception {
    var context = UnsubscribeEventContext.create();
    context.setTenant("tenant1");

    when(mockOsClient.deleteContentByPrefix("tenant1/"))
        .thenReturn(CompletableFuture.completedFuture(null));

    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant1/");
  }

  @Test
  void testCleanupTenantDataHandlesInterruptedException() throws Exception {
    var context = UnsubscribeEventContext.create();
    context.setTenant("tenant2");

    @SuppressWarnings("unchecked")
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.deleteContentByPrefix("tenant2/")).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant2/");
  }

  @Test
  void testCleanupTenantDataHandlesRuntimeException() throws Exception {
    var context = UnsubscribeEventContext.create();
    context.setTenant("tenant3");

    when(mockOsClient.deleteContentByPrefix("tenant3/"))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant3/");
  }

  @Test
  void testCleanupNullTenantThrowsServiceException() {
    var context = UnsubscribeEventContext.create();
    // tenant is null by default

    assertThrows(ServiceException.class, () -> handler.cleanupTenantData(context));
  }

  @Test
  void testCleanupHandlesExecutionException() throws Exception {
    var context = UnsubscribeEventContext.create();
    context.setTenant("tenant4");

    @SuppressWarnings("unchecked")
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.deleteContentByPrefix("tenant4/")).thenReturn(future);
    when(future.get()).thenThrow(new ExecutionException("fail", new RuntimeException("cause")));

    handler.cleanupTenantData(context);

    verify(mockOsClient).deleteContentByPrefix("tenant4/");
  }
}

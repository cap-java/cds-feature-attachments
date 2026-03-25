/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.services.mt.UnsubscribeEventContext;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantCleanupHandlerTest {

  private OSClient osClient;
  private UnsubscribeEventContext context;
  private TenantCleanupHandler handler;

  @BeforeEach
  void setUp() {
    osClient = mock(OSClient.class);
    context = mock(UnsubscribeEventContext.class);
    handler = new TenantCleanupHandler(osClient);
  }

  @Test
  void testCleanupTenantData_CallsDeleteByPrefix() throws Exception {
    String tenantId = "tenant-abc";
    when(context.getTenant()).thenReturn(tenantId);
    when(osClient.deleteContentByPrefix(tenantId + "/"))
        .thenReturn(CompletableFuture.completedFuture(null));

    handler.cleanupTenantData(context);

    verify(osClient).deleteContentByPrefix(tenantId + "/");
  }

  @Test
  void testCleanupTenantData_UsesCorrectPrefix() throws Exception {
    String tenantId = "my-tenant-123";
    when(context.getTenant()).thenReturn(tenantId);
    when(osClient.deleteContentByPrefix("my-tenant-123/"))
        .thenReturn(CompletableFuture.completedFuture(null));

    handler.cleanupTenantData(context);

    verify(osClient).deleteContentByPrefix("my-tenant-123/");
  }

  @Test
  void testCleanupTenantData_HandlesException() throws Exception {
    String tenantId = "tenant-fail";
    when(context.getTenant()).thenReturn(tenantId);

    CompletableFuture<Void> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Storage error"));
    when(osClient.deleteContentByPrefix(tenantId + "/")).thenReturn(failedFuture);

    // Should NOT rethrow — the handler logs the error but does not fail the unsubscribe
    handler.cleanupTenantData(context);

    verify(osClient).deleteContentByPrefix(tenantId + "/");
  }

  @Test
  void testCleanupTenantData_HandlesInterruptedException() throws Exception {
    String tenantId = "tenant-interrupt";
    when(context.getTenant()).thenReturn(tenantId);

    CompletableFuture<Void> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new InterruptedException("Interrupted"));
    when(osClient.deleteContentByPrefix(tenantId + "/")).thenReturn(failedFuture);

    // Should NOT rethrow — the handler logs the error but does not fail the unsubscribe
    handler.cleanupTenantData(context);

    verify(osClient).deleteContentByPrefix(tenantId + "/");
  }
}

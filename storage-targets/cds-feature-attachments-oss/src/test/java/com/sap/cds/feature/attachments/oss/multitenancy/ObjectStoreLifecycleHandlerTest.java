/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient.ServiceManagerBindingResult;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ObjectStoreLifecycleHandlerTest {

  private ServiceManagerClient smClient;
  private SeparateOSClientProvider clientProvider;
  private ObjectStoreLifecycleHandler handler;
  private static final ExecutorService executor = Executors.newCachedThreadPool();

  private static final Map<String, Object> AWS_CREDS =
      Map.of(
          "host", "s3.aws.com",
          "bucket", "tenant-bucket",
          "region", "us-east-1",
          "access_key_id", "ak",
          "secret_access_key", "sk");

  @BeforeEach
  void setup() {
    smClient = mock(ServiceManagerClient.class);
    clientProvider = mock(SeparateOSClientProvider.class);
    handler = new ObjectStoreLifecycleHandler(smClient, clientProvider, executor);
  }

  @Nested
  class Subscribe {

    @Test
    void testSubscribeCreatesInstanceAndBinding() {
      when(smClient.getBinding("t1")).thenReturn(Optional.empty());
      when(smClient.getOfferingId()).thenReturn("off-1");
      when(smClient.getPlanId("off-1")).thenReturn("plan-1");
      when(smClient.createInstance("t1", "plan-1")).thenReturn("inst-1");
      when(smClient.createBinding("t1", "inst-1"))
          .thenReturn(new ServiceManagerBindingResult("bind-1", "inst-1", AWS_CREDS));

      handler.onTenantSubscribe("t1");

      verify(smClient).createInstance("t1", "plan-1");
      verify(smClient).createBinding("t1", "inst-1");
      verify(clientProvider).put(eq("t1"), any());
    }

    @Test
    void testSubscribeIsIdempotentWhenBindingExists() {
      when(smClient.getBinding("t1"))
          .thenReturn(Optional.of(new ServiceManagerBindingResult("bind-1", "inst-1", AWS_CREDS)));

      handler.onTenantSubscribe("t1");

      verify(smClient, never()).createInstance(anyString(), anyString());
      verify(smClient, never()).createBinding(anyString(), anyString());
    }

    @Test
    void testSubscribeCleansUpInstanceOnBindingFailure() {
      when(smClient.getBinding("t1")).thenReturn(Optional.empty());
      when(smClient.getOfferingId()).thenReturn("off-1");
      when(smClient.getPlanId("off-1")).thenReturn("plan-1");
      when(smClient.createInstance("t1", "plan-1")).thenReturn("inst-1");
      doThrow(new ServiceManagerException("binding failed"))
          .when(smClient)
          .createBinding("t1", "inst-1");

      assertThrows(ServiceManagerException.class, () -> handler.onTenantSubscribe("t1"));

      verify(smClient).deleteInstance("inst-1");
    }
  }

  @Nested
  class Unsubscribe {

    @Test
    void testUnsubscribeDeletesBindingAndInstance() {
      when(smClient.getBinding("t1"))
          .thenReturn(Optional.of(new ServiceManagerBindingResult("bind-1", "inst-1", AWS_CREDS)));

      handler.onTenantUnsubscribe("t1");

      verify(clientProvider).evict("t1");
      verify(smClient).deleteBinding("bind-1");
      verify(smClient).deleteInstance("inst-1");
    }

    @Test
    void testUnsubscribeHandlesNoBindingGracefully() {
      when(smClient.getBinding("t1")).thenReturn(Optional.empty());

      handler.onTenantUnsubscribe("t1");

      verify(clientProvider).evict("t1");
      verify(smClient, never()).deleteBinding(anyString());
      verify(smClient, never()).deleteInstance(anyString());
    }

    @Test
    void testUnsubscribeContinuesOnDeleteBindingFailure() {
      when(smClient.getBinding("t1"))
          .thenReturn(Optional.of(new ServiceManagerBindingResult("bind-1", "inst-1", AWS_CREDS)));
      doThrow(new ServiceManagerException("delete failed")).when(smClient).deleteBinding("bind-1");

      handler.onTenantUnsubscribe("t1"); // should not throw

      verify(smClient).deleteInstance("inst-1"); // still attempts instance delete
    }
  }
}

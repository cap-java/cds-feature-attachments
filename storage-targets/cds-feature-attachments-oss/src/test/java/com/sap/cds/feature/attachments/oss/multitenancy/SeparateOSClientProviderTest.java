/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient.ServiceManagerBindingResult;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeparateOSClientProviderTest {

  private ServiceManagerClient smClient;
  private SeparateOSClientProvider provider;
  private static final ExecutorService executor = Executors.newCachedThreadPool();

  @BeforeEach
  void setup() {
    smClient = mock(ServiceManagerClient.class);
    provider = new SeparateOSClientProvider(smClient, executor, Duration.ofHours(11));
  }

  private void stubBinding(String tenantId) {
    Map<String, Object> creds =
        Map.of(
            "host",
            "s3.aws.com",
            "bucket",
            "b-" + tenantId,
            "region",
            "us-east-1",
            "access_key_id",
            "ak",
            "secret_access_key",
            "sk");
    when(smClient.getBinding(tenantId))
        .thenReturn(Optional.of(new ServiceManagerBindingResult("bind-1", "inst-1", creds)));
  }

  @Test
  void testGetClientCreatesClientFromSmBinding() {
    stubBinding("tenant-1");

    OSClient client = provider.getClient("tenant-1");

    assertThat(client).isNotNull();
    verify(smClient).getBinding("tenant-1");
  }

  @Test
  void testGetClientCachesClient() {
    stubBinding("tenant-1");

    OSClient first = provider.getClient("tenant-1");
    OSClient second = provider.getClient("tenant-1");

    assertThat(first).isSameAs(second);
    verify(smClient, times(1)).getBinding("tenant-1");
  }

  @Test
  void testGetClientDifferentTenants() {
    stubBinding("tenant-1");
    stubBinding("tenant-2");

    OSClient c1 = provider.getClient("tenant-1");
    OSClient c2 = provider.getClient("tenant-2");

    assertThat(c1).isNotSameAs(c2);
  }

  @Test
  void testGetClientThrowsForUnprovisionedTenant() {
    when(smClient.getBinding("unknown")).thenReturn(Optional.empty());

    assertThrows(TenantNotProvisionedException.class, () -> provider.getClient("unknown"));
  }

  @Test
  void testEvictRemovesCachedClient() {
    stubBinding("tenant-1");
    provider.getClient("tenant-1"); // warm cache

    provider.evict("tenant-1");
    provider.getClient("tenant-1"); // should fetch again

    verify(smClient, times(2)).getBinding("tenant-1");
  }

  @Test
  void testPutWarmsCache() {
    OSClient mockClient = mock(OSClient.class);
    provider.put("tenant-1", mockClient);

    OSClient result = provider.getClient("tenant-1");

    assertThat(result).isSameAs(mockClient);
    verify(smClient, times(0)).getBinding("tenant-1");
  }

  @Test
  void testExpiredEntryIsRefreshed() {
    // Use a provider with 0 TTL so entries expire immediately
    SeparateOSClientProvider shortTtl =
        new SeparateOSClientProvider(smClient, executor, Duration.ZERO);
    stubBinding("tenant-1");

    shortTtl.getClient("tenant-1");
    shortTtl.getClient("tenant-1"); // should re-fetch due to expired TTL

    verify(smClient, times(2)).getBinding("tenant-1");
  }
}

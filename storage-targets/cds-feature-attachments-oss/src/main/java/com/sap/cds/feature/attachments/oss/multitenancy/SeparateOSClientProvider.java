/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy;

import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.client.OSClientFactory;
import com.sap.cds.feature.attachments.oss.client.OSClientProvider;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerBinding;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient;
import com.sap.cds.feature.attachments.oss.multitenancy.sm.ServiceManagerClient.ServiceManagerBindingResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link OSClientProvider} for separate-bucket multitenancy mode. Each tenant gets its own
 * dedicated object store bucket, and the corresponding {@link OSClient} is cached in a {@link
 * ConcurrentHashMap} with a configurable TTL.
 */
public class SeparateOSClientProvider implements OSClientProvider {

  private static final Logger logger = LoggerFactory.getLogger(SeparateOSClientProvider.class);

  private final ConcurrentHashMap<String, CachedOSClient> cache = new ConcurrentHashMap<>();
  private final ServiceManagerClient smClient;
  private final ExecutorService executor;
  private final Duration credentialTtl;

  public SeparateOSClientProvider(
      ServiceManagerClient smClient, ExecutorService executor, Duration credentialTtl) {
    this.smClient = smClient;
    this.executor = executor;
    this.credentialTtl = credentialTtl;
  }

  @Override
  public OSClient getClient(String tenantId) {
    CachedOSClient entry =
        cache.compute(
            tenantId,
            (id, existing) -> {
              if (existing != null && !existing.isExpired(credentialTtl)) {
                return existing;
              }
              logger.debug(
                  "Resolving OSClient for tenant {} ({})",
                  id,
                  existing == null ? "cache miss" : "expired");
              return createClient(id);
            });
    return entry.client();
  }

  /** Removes the cached client for a tenant. */
  public void evict(String tenantId) {
    cache.remove(tenantId);
    logger.debug("Evicted cached OSClient for tenant {}", tenantId);
  }

  /** Warms the cache with a pre-created client for a tenant. */
  public void put(String tenantId, OSClient client) {
    cache.put(tenantId, new CachedOSClient(client, Instant.now()));
    logger.debug("Warmed cache for tenant {}", tenantId);
  }

  private CachedOSClient createClient(String tenantId) {
    ServiceManagerBindingResult binding =
        smClient
            .getBinding(tenantId)
            .orElseThrow(() -> new TenantNotProvisionedException(tenantId));
    OSClient client =
        OSClientFactory.create(new ServiceManagerBinding(binding.credentials()), executor);
    return new CachedOSClient(client, Instant.now());
  }
}

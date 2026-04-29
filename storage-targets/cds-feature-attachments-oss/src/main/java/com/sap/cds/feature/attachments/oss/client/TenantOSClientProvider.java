/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.oss.servicemanager.ServiceManagerCredentialResolver;
import com.sap.cloud.environment.servicebinding.api.DefaultServiceBindingBuilder;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe provider that resolves and caches per-tenant {@link OSClient} instances for
 * separate-bucket multitenancy mode. On cache miss, fetches the tenant's object store credentials
 * from the Service Manager and creates an {@link OSClient} via {@link OSClientFactory}.
 */
public class TenantOSClientProvider {

  private static final Logger logger = LoggerFactory.getLogger(TenantOSClientProvider.class);

  private final ServiceManagerCredentialResolver credentialResolver;
  private final ExecutorService executor;
  private final ConcurrentHashMap<String, OSClient> clientCache = new ConcurrentHashMap<>();

  /**
   * Creates a new provider.
   *
   * @param credentialResolver the resolver for fetching per-tenant credentials from Service Manager
   * @param executor the executor for async storage operations
   */
  public TenantOSClientProvider(
      ServiceManagerCredentialResolver credentialResolver, ExecutorService executor) {
    this.credentialResolver =
        requireNonNull(credentialResolver, "credentialResolver must not be null");
    this.executor = requireNonNull(executor, "executor must not be null");
  }

  /**
   * Returns the {@link OSClient} for the given tenant, creating and caching it if necessary.
   *
   * @param tenantId the tenant ID
   * @return the tenant's OSClient
   */
  public OSClient getClientForTenant(String tenantId) {
    requireNonNull(tenantId, "tenantId must not be null");
    return clientCache.computeIfAbsent(tenantId, this::createClientForTenant);
  }

  /**
   * Removes the cached {@link OSClient} for the given tenant. Called during tenant unsubscribe to
   * ensure stale clients are not reused.
   *
   * @param tenantId the tenant ID to evict
   */
  public void evict(String tenantId) {
    clientCache.remove(tenantId);
    logger.info("Evicted cached OSClient for tenant {}", tenantId);
  }

  private OSClient createClientForTenant(String tenantId) {
    logger.info("Creating OSClient for tenant {}", tenantId);

    Map<String, Object> credentials = credentialResolver.getCredentialsForTenant(tenantId);
    ServiceBinding binding =
        new DefaultServiceBindingBuilder()
            .withCredentials(credentials)
            .withServiceName("objectstore")
            .build();

    return OSClientFactory.create(binding, executor);
  }
}

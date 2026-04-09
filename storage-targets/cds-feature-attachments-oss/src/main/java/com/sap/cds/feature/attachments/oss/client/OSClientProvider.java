/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

/**
 * Strategy interface for resolving the {@link OSClient} to use for a given tenant. In shared mode,
 * a single client is returned for all tenants. In separate mode, a per-tenant client is resolved
 * from the cache or provisioned via Service Manager.
 */
public interface OSClientProvider {

  /**
   * Returns the {@link OSClient} to use for the given tenant.
   *
   * @param tenantId the tenant identifier, or {@code null} for single-tenant deployments
   * @return the appropriate {@link OSClient} instance
   */
  OSClient getClient(String tenantId);
}

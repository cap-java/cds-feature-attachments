/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import java.util.Objects;

/**
 * An {@link OSClientProvider} that always returns the same shared {@link OSClient} regardless of
 * tenant. Used for single-tenant deployments and shared-bucket multitenancy mode.
 */
public class SharedOSClientProvider implements OSClientProvider {

  private final OSClient osClient;

  public SharedOSClientProvider(OSClient osClient) {
    this.osClient = Objects.requireNonNull(osClient, "osClient must not be null");
  }

  @Override
  public OSClient getClient(String tenantId) {
    return osClient;
  }
}

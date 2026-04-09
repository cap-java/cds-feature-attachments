/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;

/**
 * Runs the separate-bucket multitenancy integration tests against real Azure Blob Storage
 * instances. Requires TWO distinct containers (one per simulated tenant). Skipped automatically if
 * the required environment variables are not set.
 *
 * <p>Required environment variables for tenant-1: {@code AZURE_CONTAINER_URI_T1}, {@code
 * AZURE_SAS_TOKEN_T1}.
 *
 * <p>Required environment variables for tenant-2: {@code AZURE_CONTAINER_URI_T2}, {@code
 * AZURE_SAS_TOKEN_T2}.
 */
class AzureSeparateBucketOssStorageTest extends AbstractSeparateBucketOssStorageTest {

  @Override
  protected ServiceBinding getTenant1ServiceBinding() {
    return buildBinding(
        System.getenv("AZURE_CONTAINER_URI_T1"), System.getenv("AZURE_SAS_TOKEN_T1"));
  }

  @Override
  protected ServiceBinding getTenant2ServiceBinding() {
    return buildBinding(
        System.getenv("AZURE_CONTAINER_URI_T2"), System.getenv("AZURE_SAS_TOKEN_T2"));
  }

  @Override
  protected String getProviderName() {
    return "Azure Blob Storage (separate containers)";
  }

  private static ServiceBinding buildBinding(String containerUri, String sasToken) {
    if (containerUri == null || sasToken == null) {
      return null;
    }

    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("container_uri", containerUri);
    creds.put("sas_token", sasToken);
    when(binding.getCredentials()).thenReturn(creds);
    return binding;
  }
}

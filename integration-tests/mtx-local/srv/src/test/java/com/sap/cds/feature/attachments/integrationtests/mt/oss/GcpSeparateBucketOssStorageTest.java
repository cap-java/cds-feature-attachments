/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;

/**
 * Runs the separate-bucket multitenancy integration tests against real Google Cloud Storage
 * instances. Requires TWO distinct GCS buckets (one per simulated tenant). Skipped automatically if
 * the required environment variables are not set.
 *
 * <p>Required environment variables for tenant-1 bucket: {@code GS_BUCKET_T1}, {@code
 * GS_PROJECT_ID}, {@code GS_BASE_64_ENCODED_PRIVATE_KEY_DATA}.
 *
 * <p>Required environment variables for tenant-2 bucket: {@code GS_BUCKET_T2} (uses the same
 * project ID and credentials as tenant-1 but a different bucket).
 */
class GcpSeparateBucketOssStorageTest extends AbstractSeparateBucketOssStorageTest {

  @Override
  protected ServiceBinding getTenant1ServiceBinding() {
    return buildBinding(System.getenv("GS_BUCKET_T1"));
  }

  @Override
  protected ServiceBinding getTenant2ServiceBinding() {
    return buildBinding(System.getenv("GS_BUCKET_T2"));
  }

  @Override
  protected String getProviderName() {
    return "Google Cloud Storage (separate buckets)";
  }

  private static ServiceBinding buildBinding(String bucket) {
    String projectId = System.getenv("GS_PROJECT_ID");
    String base64EncodedPrivateKeyData = System.getenv("GS_BASE_64_ENCODED_PRIVATE_KEY_DATA");

    if (bucket == null || projectId == null || base64EncodedPrivateKeyData == null) {
      return null;
    }

    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("bucket", bucket);
    creds.put("projectId", projectId);
    creds.put("base64EncodedPrivateKeyData", base64EncodedPrivateKeyData);
    when(binding.getCredentials()).thenReturn(creds);
    return binding;
  }
}

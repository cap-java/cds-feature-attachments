/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;

/**
 * Runs the separate-bucket multitenancy integration tests against real AWS S3 instances. Requires
 * TWO distinct S3 buckets (one per simulated tenant). Skipped automatically if the required
 * environment variables are not set.
 *
 * <p>Required environment variables for tenant-1 bucket: {@code AWS_S3_HOST}, {@code
 * AWS_S3_BUCKET_T1}, {@code AWS_S3_REGION}, {@code AWS_S3_ACCESS_KEY_ID}, {@code
 * AWS_S3_SECRET_ACCESS_KEY}.
 *
 * <p>Required environment variables for tenant-2 bucket: {@code AWS_S3_BUCKET_T2} (uses the same
 * host, region, and credentials as tenant-1 but a different bucket).
 */
class AwsSeparateBucketOssStorageTest extends AbstractSeparateBucketOssStorageTest {

  @Override
  protected ServiceBinding getTenant1ServiceBinding() {
    return buildBinding(System.getenv("AWS_S3_BUCKET_T1"));
  }

  @Override
  protected ServiceBinding getTenant2ServiceBinding() {
    return buildBinding(System.getenv("AWS_S3_BUCKET_T2"));
  }

  @Override
  protected String getProviderName() {
    return "AWS S3 (separate buckets)";
  }

  private static ServiceBinding buildBinding(String bucket) {
    String host = System.getenv("AWS_S3_HOST");
    String region = System.getenv("AWS_S3_REGION");
    String accessKeyId = System.getenv("AWS_S3_ACCESS_KEY_ID");
    String secretAccessKey = System.getenv("AWS_S3_SECRET_ACCESS_KEY");

    if (host == null
        || bucket == null
        || region == null
        || accessKeyId == null
        || secretAccessKey == null) {
      return null;
    }

    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("host", host);
    creds.put("bucket", bucket);
    creds.put("region", region);
    creds.put("access_key_id", accessKeyId);
    creds.put("secret_access_key", secretAccessKey);
    when(binding.getCredentials()).thenReturn(creds);
    return binding;
  }
}

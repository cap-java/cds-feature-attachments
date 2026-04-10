/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;

/**
 * Runs the multitenancy OSS storage integration tests against a real AWS S3 instance. Skipped
 * automatically if the required environment variables are not set.
 *
 * <p>Required environment variables: {@code AWS_S3_HOST}, {@code AWS_S3_BUCKET}, {@code
 * AWS_S3_REGION}, {@code AWS_S3_ACCESS_KEY_ID}, {@code AWS_S3_SECRET_ACCESS_KEY}.
 */
class AwsMtxOssStorageTest extends AbstractMtxOssStorageTest {

  @Override
  protected ServiceBinding getServiceBinding() {
    String host = System.getenv("AWS_S3_HOST");
    String bucket = System.getenv("AWS_S3_BUCKET");
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

  @Override
  protected String getProviderName() {
    return "AWS S3";
  }
}

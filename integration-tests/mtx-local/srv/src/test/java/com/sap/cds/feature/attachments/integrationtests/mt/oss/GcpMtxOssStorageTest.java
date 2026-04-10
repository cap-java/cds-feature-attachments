/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;

/**
 * Runs the multitenancy OSS storage integration tests against a real Google Cloud Storage instance.
 * Skipped automatically if the required environment variables are not set.
 *
 * <p>Required environment variables: {@code GS_BUCKET}, {@code GS_PROJECT_ID}, {@code
 * GS_BASE_64_ENCODED_PRIVATE_KEY_DATA}.
 */
class GcpMtxOssStorageTest extends AbstractMtxOssStorageTest {

  @Override
  protected ServiceBinding getServiceBinding() {
    String bucket = System.getenv("GS_BUCKET");
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

  @Override
  protected String getProviderName() {
    return "Google Cloud Storage";
  }
}

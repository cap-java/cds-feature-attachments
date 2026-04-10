/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.mt.oss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;

/**
 * Runs the multitenancy OSS storage integration tests against a real Azure Blob Storage instance.
 * Skipped automatically if the required environment variables are not set.
 *
 * <p>Required environment variables: {@code AZURE_CONTAINER_URI}, {@code AZURE_SAS_TOKEN}.
 */
class AzureMtxOssStorageTest extends AbstractMtxOssStorageTest {

  @Override
  protected ServiceBinding getServiceBinding() {
    String containerUri = System.getenv("AZURE_CONTAINER_URI");
    String sasToken = System.getenv("AZURE_SAS_TOKEN");

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

  @Override
  protected String getProviderName() {
    return "Azure Blob Storage";
  }
}

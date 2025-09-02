/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.utility.DockerImageName;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

class AzureClientIT {

  // The tests in this class are intended to run against a real Azure instance.
  // They require a valid ServiceBinding with credentials set up in the environment.

  private static AzuriteContainer container =
      new AzuriteContainer(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:latest"));

  @BeforeAll
  static void beforeAll() {
    container.start();
  }

  @AfterAll
  static void tearDown() {
    if (container != null) {
      container.stop();
    }
  }

  @Test
  void testCreateReadDeleteAttachmentFlowAzure() throws Exception {
    ServiceBinding binding = getRealServiceBindingAzure();
    ExecutorService executor = Executors.newCachedThreadPool();

    OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding, executor);
  }

  private ServiceBinding getRealServiceBindingAzure() {
    // Read environment variables
    String containerUri = container.getConnectionString(); // System.getenv("AZURE_CONTAINER_URI");
    String sasToken = ""; // System.getenv("AZURE_SAS_TOKEN");
    String[] tokens = container.getConnectionString().split(";");
    for (String token : tokens) {
      if (token.startsWith("BlobEndpoint")) {
        String[] blobTokens = token.split("=");
        containerUri = blobTokens[0];
      }
    }
    // Return null if any are missing
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

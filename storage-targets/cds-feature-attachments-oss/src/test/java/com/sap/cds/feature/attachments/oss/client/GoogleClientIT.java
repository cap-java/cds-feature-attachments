/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class GoogleClientIT {
  // The tests in this class are intended to run against a real Google Cloud Storage instance.
  // They require a valid ServiceBinding with credentials set up in the environment.

  @Test
  void testCreateReadDeleteAttachmentFlowGoogle() throws Exception {
    ServiceBinding binding = getRealServiceBindingGoogle();
    ExecutorService executor = Executors.newCachedThreadPool();

    OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding, executor);
  }

  private ServiceBinding getRealServiceBindingGoogle() {
    // Read environment variables
    String bucket = System.getenv("GS_BUCKET");
    String projectId = System.getenv("GS_PROJECT_ID");
    String base64EncodedPrivateKeyData = System.getenv("GS_BASE_64_ENCODED_PRIVATE_KEY_DATA");

    // Return null if any are missing
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

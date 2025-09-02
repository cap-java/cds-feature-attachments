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

public class AWSClientIT {
  // The tests in this class are intended to run against a real AWS Storage instance.
  // They require a valid ServiceBinding with credentials set up in the environment.

  @Test
  void testCreateReadDeleteAttachmentFlowAWS() throws Exception {
    ServiceBinding binding = getRealServiceBindingAWS();
    ExecutorService executor = Executors.newCachedThreadPool();
    OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding, executor);
  }

  private ServiceBinding getRealServiceBindingAWS() {
    // Read environment variables
    String host = System.getenv("AWS_S3_HOST");
    String bucket = System.getenv("AWS_S3_BUCKET");
    String region = System.getenv("AWS_S3_REGION");
    String accessKeyId = System.getenv("AWS_S3_ACCESS_KEY_ID");
    String secretAccessKey = System.getenv("AWS_S3_SECRET_ACCESS_KEY");

    // Return null if any are missing
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

package com.sap.cds.feature.attachments.oss.client;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class GoogleClientIntegrationTest {
    // The tests in this class are intended to run against a real Google Cloud Storage instance.
    // They require a valid ServiceBinding with credentials set up in the environment.
    // For this reason, the tests are not run when no real binding is available.

    @Test
    void testCreateReadDeleteAttachmentFlowGoogle() throws Exception {
        ServiceBinding binding = getRealServiceBindingGoogle();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding);
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

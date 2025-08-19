package com.sap.cds.feature.attachments.oss.client;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AzureClientIntegrationTest {
    // The tests in this class are intended to run against a real Azure instance.
    // They require a valid ServiceBinding with credentials set up in the environment.
    // For this reason, the tests are not run when no real binding is available.

    @Test
    void testCreateReadDeleteAttachmentFlowAzure() throws Exception {
        ServiceBinding binding = getRealServiceBindingAzure();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding);
    }

    private ServiceBinding getRealServiceBindingAzure() {
        // Read environment variables
        String containerUri = System.getenv("AZURE_CONTAINER_URI");
        String sasToken = System.getenv("AZURE_SAS_TOKEN");
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

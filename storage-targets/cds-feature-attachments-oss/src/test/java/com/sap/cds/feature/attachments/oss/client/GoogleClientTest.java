package com.sap.cds.feature.attachments.oss.client;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class GoogleClientTest {
    @Test
    void testCreateReadDeleteAttachmentFlowGoogle() throws Exception {
        ServiceBinding binding = getRealServiceBindingGoogle();
        OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding);
    }

    @Test
    void testConstructorThrowsServiceExceptionOnInvalidKey() {
        // Arrange: create a ServiceBinding with invalid base64 key data
        ServiceBinding binding = mock(ServiceBinding.class);
        HashMap<String, Object> creds = new HashMap<>();
        creds.put("bucket", "dummy-bucket");
        creds.put("projectId", "dummy-project");
        // This is "dummy-json" as base64: ZHVtbXktanNvbg==
        creds.put("base64EncodedPrivateKeyData", "ZHVtbXktanNvbg==");
        when(binding.getCredentials()).thenReturn(creds);

        // Act & Assert
        ServiceException thrown = assertThrows(ServiceException.class, () -> new GoogleClient(binding));
        assertTrue(thrown.getMessage().contains("Failed to initialize Google Cloud Storage client"));
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

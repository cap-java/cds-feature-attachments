package com.sap.cds.feature.attachments.oss.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobOutputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AzureClientTest {
    @Test
    void testCreateReadDeleteAttachmentFlowAzure() throws Exception {
        ServiceBinding binding = getRealServiceBindingAzure();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding);
    }

    @Test
    void testUploadContentThrowsOnIOException() throws NoSuchFieldException, IllegalAccessException {
        ServiceBinding binding = getRealServiceBindingAzure();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        AzureClient azureClient = spy(new AzureClient(binding));

        // Mock BlobContainerClient and BlockBlobClient
        BlobContainerClient mockContainer = mock(BlobContainerClient.class);
        BlockBlobClient mockBlockBlob = mock(BlockBlobClient.class);
        BlobOutputStream mockOutputStream = mock(BlobOutputStream.class);

        var field = AzureClient.class.getDeclaredField("blobContainerClient");
        field.setAccessible(true);
        field.set(azureClient, mockContainer);
        when(mockContainer.getBlobClient(anyString())).thenReturn(mock(BlobClient.class));
        when(mockContainer.getBlobClient(anyString()).getBlockBlobClient()).thenReturn(mockBlockBlob);
        when(mockBlockBlob.getBlobOutputStream()).thenReturn(mockOutputStream);

        // Mock InputStream to throw IOException
        InputStream mockInput = mock(InputStream.class);
        try {
            when(mockInput.read(any(byte[].class))).thenThrow(new IOException("Simulated IO failure"));
        } catch (IOException e) {
            // This will not happen
        }

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
            azureClient.uploadContent(mockInput, "file.txt", "text/plain").get()
        );
        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
    }

        @Test
    void testDeleteContentThrowsOnRuntimeException() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        ServiceBinding binding = getRealServiceBindingAzure();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        AzureClient azureClient = spy(new AzureClient(binding));

        // Mock BlobContainerClient and BlobClient
        BlobContainerClient mockContainer = mock(BlobContainerClient.class);
        BlobClient mockBlobClient = mock(BlobClient.class);

        var field = AzureClient.class.getDeclaredField("blobContainerClient");
        field.setAccessible(true);
        field.set(azureClient, mockContainer);
        when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlobClient);

        // Mock delete to throw RuntimeException
        doThrow(new RuntimeException("Simulated delete failure")).when(mockBlobClient).delete();

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
            azureClient.deleteContent("file.txt").get()
        );
        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
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

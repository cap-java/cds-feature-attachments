package com.sap.cds.feature.attachments.oss.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
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
        String plain = "this is just a dummy string without keywords";
        String base64 = Base64.getEncoder().encodeToString(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        creds.put("base64EncodedPrivateKeyData", base64);
        when(binding.getCredentials()).thenReturn(creds);

        // Act & Assert
        ServiceException thrown = assertThrows(ServiceException.class, () -> new GoogleClient(binding));
        assertTrue(thrown.getMessage().contains("Failed to initialize Google Cloud Storage client"));
    }
    
    @Test
    void testUploadContentThrowsServiceExceptionOnIOException() throws Exception {
        ServiceBinding binding = getRealServiceBindingGoogle();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        GoogleClient googleClient = spy(new GoogleClient(binding));

        // Mock storage and writer to throw IOException
        Storage mockStorage = mock(Storage.class);
        WriteChannel mockWriter = mock(WriteChannel.class);

        // Inject mock storage into googleClient using reflection
        var field = GoogleClient.class.getDeclaredField("storage");
        field.setAccessible(true);
        field.set(googleClient, mockStorage);

        when(mockStorage.writer(any(BlobInfo.class))).thenReturn(mockWriter);
        doThrow(new IOException("Simulated IO failure")).when(mockWriter).write(any(java.nio.ByteBuffer.class));

        InputStream input = mock(InputStream.class);
        try {
            when(input.read(any(byte[].class))).thenReturn(1).thenThrow(new IOException("Simulated IO failure"));
        } catch (IOException e) {
            // Will not happen in mock setup
        }

        CompletionException thrown = assertThrows(CompletionException.class, () ->
            googleClient.uploadContent(input, "file.txt", "text/plain").join()
        );
        assertTrue(thrown.getCause() instanceof ServiceException);
        assertTrue(thrown.getCause().getMessage().contains("Failed to upload file"));
    }

    @Test
    void testDeleteContentThrowsServiceExceptionOnRuntimeException() throws Exception {
        ServiceBinding binding = getRealServiceBindingGoogle();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        GoogleClient googleClient = spy(new GoogleClient(binding));

        // Mock storage and blob to throw RuntimeException on delete
        Storage mockStorage = mock(Storage.class);
        Blob mockBlob = mock(Blob.class);

        // Inject mock storage into googleClient using reflection
        var field = GoogleClient.class.getDeclaredField("storage");
        field.setAccessible(true);
        field.set(googleClient, mockStorage);

        when(mockStorage.get(any(String.class), any(String.class))).thenReturn(mockBlob);
        doThrow(new RuntimeException("Simulated delete failure")).when(mockBlob).delete();

        CompletionException thrown = assertThrows(CompletionException.class, () ->
            googleClient.deleteContent("file.txt").join()
        );
        assertTrue(thrown.getCause() instanceof ServiceException);
        assertTrue(thrown.getCause().getMessage().contains("Failed to delete file"));
    }

    @Test
    void testReadContentThrowsServiceExceptionOnRuntimeException() throws Exception {
        ServiceBinding binding = getRealServiceBindingGoogle();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        GoogleClient googleClient = spy(new GoogleClient(binding));

        // Mock storage and blob to throw RuntimeException on reader
        Storage mockStorage = mock(Storage.class);

        // Inject mock storage into googleClient using reflection
        var field = GoogleClient.class.getDeclaredField("storage");
        field.setAccessible(true);
        field.set(googleClient, mockStorage);

        // Mock blob.reader() to throw RuntimeException
        doThrow(new RuntimeException("Simulated read failure"))
            .when(mockStorage).reader(any(com.google.cloud.storage.BlobId.class));

        CompletionException thrown = assertThrows(CompletionException.class, () ->
            googleClient.readContent("file.txt").join()
        );
        assertTrue(thrown.getCause() instanceof ServiceException);
        assertTrue(thrown.getCause().getMessage().contains("Failed to read file"));
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

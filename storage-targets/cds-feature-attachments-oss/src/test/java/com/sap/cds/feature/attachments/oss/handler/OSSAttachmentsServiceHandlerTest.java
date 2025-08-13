package com.sap.cds.feature.attachments.oss.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.oss.client.AWSClient;
import com.sap.cds.feature.attachments.oss.client.MockOSClient;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;



class OSSAttachmentsServiceHandlerTest {

    @Test
    void testConstructorWithNoBindingUsesMockClient() throws NoSuchFieldException, IllegalAccessException {
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.empty());
        // Reflection to access private static osClient
        OSClient client = getOsClient(handler);
        assertTrue(client instanceof MockOSClient);
    }

    @Test
    // Dummy tests for the other two clients (Azure and Google) are omitted because there a real binding is needed!
    void testConstructorWithAwsBindingUsesAwsClient() throws NoSuchFieldException, IllegalAccessException {
        // Create a mock ServiceBinding with AWS credentials
        ServiceBinding binding = mock(ServiceBinding.class);
        HashMap<String, Object> creds = new HashMap<>();
        creds.put("host", "s3.amazonaws.com");
		creds.put("bucket", "dummy");
		creds.put("region", "dummy");
		creds.put("access_key_id", "dummy");
		creds.put("secret_access_key", "dummy");
		when(binding.getCredentials()).thenReturn(creds);

        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(binding));
        OSClient client = getOsClient(handler);
        assertTrue(client instanceof AWSClient);
    }

    @Test
    void testCreateReadDeleteAttachmentFlowAWS() throws Exception {
        ServiceBinding binding = getRealServiceBindingAWS();

        testCreateReadDeleteAttachmentFlow(binding);
    }

    @Test
    void testCreateReadDeleteAttachmentFlowAzure() throws Exception {
        ServiceBinding binding = getRealServiceBindingAzure();
        testCreateReadDeleteAttachmentFlow(binding);
    }

    @Test
    void testCreateReadDeleteAttachmentFlowGoogle() throws Exception {
        ServiceBinding binding = getRealServiceBindingGoogle();
        testCreateReadDeleteAttachmentFlow(binding);
    }

    // This methods tests the complete flow of creating, reading, and deleting an attachment
    // for all OS clients. It uses a mock ServiceBinding to simulate the attachment service.
    private void testCreateReadDeleteAttachmentFlow(ServiceBinding binding) throws Exception {
        // Create test file to upload, read and delete
        String testFileName = "testFileName-" + System.currentTimeMillis() + ".txt";
        String testFileContent = "test";
		
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(binding));

		// Create an AttachmentCreateEventContext with mocked data - to upload a test attachment
        MediaData createMediaData = mock(MediaData.class);
		when(createMediaData.getMimeType()).thenReturn("text/plain");
        InputStream content = new ByteArrayInputStream(testFileContent.getBytes());
        when(createMediaData.getContent()).thenReturn(content);

        CdsEntity attachmentEntity = mock(CdsEntity.class);
        when(attachmentEntity.getQualifiedName()).thenReturn(testFileName);
        
		AttachmentCreateEventContext createContext = mock(AttachmentCreateEventContext.class);
        when(createContext.getData()).thenReturn(createMediaData);
        when(createContext.getAttachmentEntity()).thenReturn(attachmentEntity);
        when(createContext.getAttachmentIds()).thenReturn(new HashMap<>() {{
            put(Attachments.ID, testFileName);
        }});
        doNothing().when(createContext).setCompleted();

        handler.createAttachment(createContext);
        // Verify that the function setCompleted was called
        verify(createContext).setCompleted();

        // Now read attachment
        MediaData readMediaData = mock(MediaData.class);
        // When calling readAttachment, we modify the readMetaData by calling setContent and setStatus.
        // To check if these functions are called correctly, we use Mockito's doAnswer to capture the arguments passed to these methods.
        doAnswer(invocation -> {
            InputStream receivedInputStream = invocation.getArgument(0);
            assertEquals(testFileContent, new String(receivedInputStream.readAllBytes()));
            return null;
        }).when(readMediaData).setContent(any());
        doAnswer(invocation -> {
            String newStatus = invocation.getArgument(0);
            assertEquals(newStatus, StatusCode.CLEAN);
            return null;
        }).when(readMediaData).setStatus(any());

        AttachmentReadEventContext readContext = mock(AttachmentReadEventContext.class);
        when(readContext.getContentId()).thenReturn(testFileName);
        when(readContext.getData()).thenReturn(readMediaData);
        doNothing().when(readContext).setCompleted();
        
        handler.readAttachment(readContext);
        // Verify that the function setCompleted was called
        verify(readContext).setCompleted();

        // Delete attachment
        AttachmentMarkAsDeletedEventContext deleteContext = mock(AttachmentMarkAsDeletedEventContext.class);
        when(deleteContext.getContentId()).thenReturn(testFileName);
        doNothing().when(readContext).setCompleted();

        handler.markAttachmentAsDeleted(deleteContext);
        // Verify that the function setCompleted was called
        verify(deleteContext).setCompleted();

        // Try to read again, this will throw a exception
       assertThrows(RuntimeException.class, () -> handler.readAttachment(readContext));
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

    private ServiceBinding getRealServiceBindingAWS() {
        // Read environment variables
        String host = System.getenv("AWS_S3_HOST");
        String bucket = System.getenv("AWS_S3_BUCKET");
        String region = System.getenv("AWS_S3_REGION");
        String accessKeyId = System.getenv("AWS_S3_ACCESS_KEY_ID");
        String secretAccessKey = System.getenv("AWS_S3_SECRET_ACCESS_KEY");

        // Return null if any are missing
        if (host == null || bucket == null || region == null || accessKeyId == null || secretAccessKey == null) {
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

    // Helper to access private static osClient
    private OSClient getOsClient(OSSAttachmentsServiceHandler handler) throws NoSuchFieldException, IllegalAccessException {
        var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
        field.setAccessible(true);
        return (OSClient) field.get(handler);
    }
}

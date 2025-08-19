package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class AWSClientTest {

    @Test
    // Dummy tests for the other two clients (Azure and Google) are omitted because there a real binding is needed!
    void testConstructorWithAwsBindingUsesAwsClient() throws NoSuchFieldException, IllegalAccessException {
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(getDummyBinding()));
        OSClient client = OSSAttachmentsServiceHandlerTestUtils.getOsClient(handler);
        assertTrue(client instanceof AWSClient);
    }

    @Test
    void testCreateReadDeleteAttachmentFlowAWS() throws Exception {
        ServiceBinding binding = getRealServiceBindingAWS();
        if (binding == null) {
            // Skip the test if no real binding is available
            return;
        }
        OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding);
    }

    @Test
    void testUploadContentThrowsAWS() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding());

        // Inject a mock S3AsyncClient that always fails
        S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Simulated S3 failure"));
        when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn((CompletableFuture) failedFuture);

        // Use reflection to set the private s3AsyncClient field
        var field = AWSClient.class.getDeclaredField("s3AsyncClient");
        field.setAccessible(true);
        field.set(awsClient, mockAsyncClient);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> 
            awsClient.uploadContent(
                new ByteArrayInputStream("test".getBytes()), 
                "test.txt", 
                "text/plain"
            ).get()
        );

        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
    }

    @Test
    void testUploadContentThrowsAWSOnPutResponseNull() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding());

        // Inject a mock S3AsyncClient that returns a completed future with null
        S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
        CompletableFuture<PutObjectResponse> nullFuture = CompletableFuture.completedFuture(null);
        when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(nullFuture);

        // Use reflection to set the private s3AsyncClient field
        var field = AWSClient.class.getDeclaredField("s3AsyncClient");
        field.setAccessible(true);
        field.set(awsClient, mockAsyncClient);

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
            awsClient.uploadContent(
                new ByteArrayInputStream("test".getBytes()),
                "test.txt",
                "text/plain"
            ).get()
        );

        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
    }

    @Test
    void testDeleteContentThrowsOnRuntimeException() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding());

        // Mock S3Client to throw a RuntimeException
        S3Client mockS3Client = mock(S3Client.class);
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(new RuntimeException("Simulated S3 delete failure"));

        // Inject mock S3Client
        var field = AWSClient.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(awsClient, mockS3Client);

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
            awsClient.deleteContent("test.txt").get()
        );
        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
    }

    @Test
    void testDeleteContentThrowsOnUnsuccessfulResponse() throws NoSuchFieldException, IllegalAccessException {
        AWSClient awsClient = new AWSClient(getDummyBinding());

        // Mock S3Client to return a DeleteObjectResponse with unsuccessful SdkHttpResponse
        S3Client mockS3Client = mock(S3Client.class);
        DeleteObjectResponse mockDelRes = mock(DeleteObjectResponse.class);
        SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
        when(mockHttpRes.isSuccessful()).thenReturn(false);
        when(mockHttpRes.statusText()).thenReturn(Optional.of("Simulated failure"));
        when(mockDelRes.sdkHttpResponse()).thenReturn(mockHttpRes);
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockDelRes);

        // Inject mock S3Client
        var field = AWSClient.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(awsClient, mockS3Client);

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
            awsClient.deleteContent("test.txt").get()
        );
        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
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

    private ServiceBinding getDummyBinding() {
        ServiceBinding binding = mock(ServiceBinding.class);
        HashMap<String, Object> creds = new HashMap<>();
        creds.put("host", "s3.amazonaws.com");
        creds.put("bucket", "dummy-bucket");
        creds.put("region", "eu-central-1");
        creds.put("access_key_id", "dummy");
        creds.put("secret_access_key", "dummy");
        when(binding.getCredentials()).thenReturn(creds);
        return binding;
    }

}

package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class AWSClientTest {
    ExecutorService executor = Executors.newCachedThreadPool();

    @Test
    void testConstructorWithAwsBindingUsesAwsClient() throws NoSuchFieldException, IllegalAccessException {
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(getDummyBinding()), executor);
        OSClient client = OSSAttachmentsServiceHandlerTestUtils.getOsClient(handler);
        assertTrue(client instanceof AWSClient);
    }

    @Test
    void testReadContent() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3Client to return a dummy InputStream
        S3Client mockS3Client = mock(S3Client.class);
        ByteArrayInputStream mockInputStream = new ByteArrayInputStream("test-data".getBytes());
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        ResponseInputStream<GetObjectResponse> mockResponseInputStream = new ResponseInputStream<>(mockResponse, mockInputStream);

        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockResponseInputStream);
        
        var field = AWSClient.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(awsClient, mockS3Client);

        InputStream result = awsClient.readContent("test.txt").get();
        assertNotNull(result);
    }

    @Test
    void testUploadContent() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3AsyncClient to return a successful PutObjectResponse
        S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
        PutObjectResponse mockPutRes = mock(PutObjectResponse.class);
        SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
        when(mockHttpRes.isSuccessful()).thenReturn(true);
        when(mockPutRes.sdkHttpResponse()).thenReturn(mockHttpRes);
        CompletableFuture<PutObjectResponse> successFuture = CompletableFuture.completedFuture(mockPutRes);
        when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(successFuture);

        var field = AWSClient.class.getDeclaredField("s3AsyncClient");
        field.setAccessible(true);
        field.set(awsClient, mockAsyncClient);

        // Should not throw
        
        awsClient.uploadContent(
            new ByteArrayInputStream("test".getBytes()),
            "test.txt",
            "text/plain"
        ).get(); 
    }

    @Test
    void testDeleteContent() throws NoSuchFieldException, IllegalAccessException {
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3Client to return a DeleteObjectResponse with successful SdkHttpResponse
        S3Client mockS3Client = mock(S3Client.class);
        DeleteObjectResponse mockDelRes = mock(DeleteObjectResponse.class);
        SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
        when(mockHttpRes.isSuccessful()).thenReturn(true);
        when(mockDelRes.sdkHttpResponse()).thenReturn(mockHttpRes);
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockDelRes);

        var field = AWSClient.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(awsClient, mockS3Client);

        assertDoesNotThrow(() -> awsClient.deleteContent("test.txt").get());
    }

    @Test
    void testReadContentThrows() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3Client to return a dummy InputStream
        S3Client mockS3Client = mock(S3Client.class);

        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("Simulated S3 failure"));
        
        var field = AWSClient.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(awsClient, mockS3Client);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> awsClient.readContent("test.txt").get());
        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
    }

    @Test
    void testUploadContentThrows() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3AsyncClient that always fails
        S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
        CompletableFuture<?> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Simulated S3 failure"));
        when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn((CompletableFuture) failedFuture);

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
    void testUploadContentThrowsOnPutResponseNull() throws Exception {
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3AsyncClient that returns a null PutObjectResponse
        S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
        CompletableFuture<PutObjectResponse> nullFuture = CompletableFuture.completedFuture(null);
        when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
            .thenReturn(nullFuture);

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
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3Client to throw a RuntimeException
        S3Client mockS3Client = mock(S3Client.class);
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(new RuntimeException("Simulated S3 delete failure"));

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
        AWSClient awsClient = new AWSClient(getDummyBinding(), executor);

        // Mock S3Client to return a DeleteObjectResponse with unsuccessful SdkHttpResponse
        S3Client mockS3Client = mock(S3Client.class);
        DeleteObjectResponse mockDelRes = mock(DeleteObjectResponse.class);
        SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
        when(mockHttpRes.isSuccessful()).thenReturn(false);
        when(mockDelRes.sdkHttpResponse()).thenReturn(mockHttpRes);
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockDelRes);

        var field = AWSClient.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(awsClient, mockS3Client);

        ExecutionException thrown = assertThrows(ExecutionException.class, () ->
            awsClient.deleteContent("test.txt").get()
        );
        assertTrue(thrown.getCause() instanceof ObjectStoreServiceException);
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

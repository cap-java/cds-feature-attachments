/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class AWSClientTest {
  ExecutorService executor = Executors.newCachedThreadPool();

  @Test
  void testConstructorWithAwsBindingUsesAwsClient()
      throws NoSuchFieldException, IllegalAccessException {
    OSSAttachmentsServiceHandler handler =
        new OSSAttachmentsServiceHandler(getDummyBinding(), executor, false, null);
    OSClient client = OSSAttachmentsServiceHandlerTestUtils.getOsClient(handler);
    assertInstanceOf(AWSClient.class, client);
  }

  @Test
  void testReadContent() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    ByteArrayInputStream mockInputStream = new ByteArrayInputStream("test-data".getBytes());
    GetObjectResponse mockResponse = mock(GetObjectResponse.class);
    ResponseInputStream<GetObjectResponse> mockResponseInputStream =
        new ResponseInputStream<>(mockResponse, mockInputStream);

    when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockResponseInputStream);

    InputStream result = awsClient.readContent("test.txt").get();
    assertNotNull(result);
  }

  @Test
  void testUploadContent() throws Exception {
    S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
    AWSClient awsClient = new AWSClient(mock(S3Client.class), mockAsyncClient, "bucket", executor);

    PutObjectResponse mockPutRes = mock(PutObjectResponse.class);
    SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
    when(mockHttpRes.isSuccessful()).thenReturn(true);
    when(mockPutRes.sdkHttpResponse()).thenReturn(mockHttpRes);
    CompletableFuture<PutObjectResponse> successFuture =
        CompletableFuture.completedFuture(mockPutRes);
    when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
        .thenReturn(successFuture);

    awsClient
        .uploadContent(new ByteArrayInputStream("test".getBytes()), "test.txt", "text/plain")
        .get();
  }

  @Test
  void testDeleteContent() {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    DeleteObjectResponse mockDelRes = mock(DeleteObjectResponse.class);
    SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
    when(mockHttpRes.isSuccessful()).thenReturn(true);
    when(mockDelRes.sdkHttpResponse()).thenReturn(mockHttpRes);
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockDelRes);

    assertDoesNotThrow(() -> awsClient.deleteContent("test.txt").get());
  }

  @Test
  void testReadContentThrows() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    when(mockS3Client.getObject(any(GetObjectRequest.class)))
        .thenThrow(new RuntimeException("Simulated S3 failure"));

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> awsClient.readContent("test.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testUploadContentThrows() throws Exception {
    S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
    AWSClient awsClient = new AWSClient(mock(S3Client.class), mockAsyncClient, "bucket", executor);

    CompletableFuture<?> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Simulated S3 failure"));
    when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
        .thenReturn((CompletableFuture) failedFuture);

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () ->
                awsClient
                    .uploadContent(
                        new ByteArrayInputStream("test".getBytes()), "test.txt", "text/plain")
                    .get());

    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testUploadContentThrowsOnPutResponseNull() throws Exception {
    S3AsyncClient mockAsyncClient = mock(S3AsyncClient.class);
    AWSClient awsClient = new AWSClient(mock(S3Client.class), mockAsyncClient, "bucket", executor);

    CompletableFuture<PutObjectResponse> nullFuture = CompletableFuture.completedFuture(null);
    when(mockAsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
        .thenReturn(nullFuture);

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () ->
                awsClient
                    .uploadContent(
                        new ByteArrayInputStream("test".getBytes()), "test.txt", "text/plain")
                    .get());

    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContentThrowsOnRuntimeException() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException("Simulated S3 delete failure"));

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> awsClient.deleteContent("test.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContentThrowsOnUnsuccessfulResponse() {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    DeleteObjectResponse mockDelRes = mock(DeleteObjectResponse.class);
    SdkHttpResponse mockHttpRes = mock(SdkHttpResponse.class);
    when(mockHttpRes.isSuccessful()).thenReturn(false);
    when(mockDelRes.sdkHttpResponse()).thenReturn(mockHttpRes);
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(mockDelRes);

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> awsClient.deleteContent("test.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContentByPrefix() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    S3Object obj1 = S3Object.builder().key("prefix/file1.txt").build();
    S3Object obj2 = S3Object.builder().key("prefix/file2.txt").build();

    ListObjectsV2Response listResponse = mock(ListObjectsV2Response.class);
    when(listResponse.contents()).thenReturn(List.of(obj1, obj2));
    when(listResponse.isTruncated()).thenReturn(false);
    when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

    DeleteObjectsResponse deleteResponse = mock(DeleteObjectsResponse.class);
    when(deleteResponse.hasErrors()).thenReturn(false);
    when(deleteResponse.errors()).thenReturn(Collections.emptyList());
    when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse);

    awsClient.deleteContentByPrefix("prefix/").get();

    verify(mockS3Client).deleteObjects(any(DeleteObjectsRequest.class));
  }

  @Test
  void testDeleteContentByPrefixEmptyList() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    ListObjectsV2Response listResponse = mock(ListObjectsV2Response.class);
    when(listResponse.contents()).thenReturn(Collections.emptyList());
    when(listResponse.isTruncated()).thenReturn(false);
    when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

    assertDoesNotThrow(() -> awsClient.deleteContentByPrefix("prefix/").get());
  }

  @Test
  void testDeleteContentByPrefixThrowsOnRuntimeException() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenThrow(new RuntimeException("Simulated failure"));

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class, () -> awsClient.deleteContentByPrefix("prefix/").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContentByPrefixWithPagination() throws Exception {
    S3Client mockS3Client = mock(S3Client.class);
    AWSClient awsClient = new AWSClient(mockS3Client, mock(S3AsyncClient.class), "bucket", executor);

    // First page: 2 objects, isTruncated=true
    S3Object obj1 = S3Object.builder().key("prefix/file1.txt").build();
    S3Object obj2 = S3Object.builder().key("prefix/file2.txt").build();

    ListObjectsV2Response firstPage = mock(ListObjectsV2Response.class);
    when(firstPage.contents()).thenReturn(List.of(obj1, obj2));
    when(firstPage.isTruncated()).thenReturn(true);
    when(firstPage.nextContinuationToken()).thenReturn("token1");

    // Second page: 1 object, isTruncated=false
    S3Object obj3 = S3Object.builder().key("prefix/file3.txt").build();

    ListObjectsV2Response secondPage = mock(ListObjectsV2Response.class);
    when(secondPage.contents()).thenReturn(List.of(obj3));
    when(secondPage.isTruncated()).thenReturn(false);

    // First call returns first page, second call (with token) returns second page
    when(mockS3Client.listObjectsV2(
            argThat((ListObjectsV2Request req) -> req != null && req.continuationToken() == null)))
        .thenReturn(firstPage);
    when(mockS3Client.listObjectsV2(
            argThat(
                (ListObjectsV2Request req) ->
                    req != null && "token1".equals(req.continuationToken()))))
        .thenReturn(secondPage);

    DeleteObjectsResponse deleteResponse = mock(DeleteObjectsResponse.class);
    when(deleteResponse.hasErrors()).thenReturn(false);
    when(deleteResponse.errors()).thenReturn(Collections.emptyList());
    when(mockS3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteResponse);

    awsClient.deleteContentByPrefix("prefix/").get();

    // deleteObjects should be called twice — once per page
    verify(mockS3Client, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
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

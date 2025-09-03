/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class GoogleClientTest {
  ExecutorService executor = Executors.newCachedThreadPool();

  @Test
  void testConstructorThrowsOnInvalidCredentials() {
    ServiceBinding mockBinding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("bucket", "bucket");
    creds.put("projectId", "project");
    creds.put(
        "base64EncodedPrivateKeyData", Base64.getEncoder().encodeToString("dummy".getBytes()));
    when(mockBinding.getCredentials()).thenReturn(creds);

    var mocked = mockStatic(com.google.auth.oauth2.ServiceAccountCredentials.class);
    mocked
        .when(
            () ->
                com.google.auth.oauth2.ServiceAccountCredentials.fromStream(any(InputStream.class)))
        .thenThrow(new IOException("Simulated IO error"));

    ObjectStoreServiceException ex =
        assertThrows(
            ObjectStoreServiceException.class, () -> new GoogleClient(mockBinding, executor));
    assertInstanceOf(IOException.class, ex.getCause());
  }

  @Test
  void testDeleteContent()
      throws NoSuchFieldException,
          IllegalArgumentException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    String fileName = "file.txt";

    // Mock storage and paging
    Storage mockStorage = mock(Storage.class);
    Page<Blob> mockPage = mock(Page.class);
    Blob mockBlob = mock(Blob.class);
    when(mockBlob.getName()).thenReturn(fileName);
    when(mockBlob.getGeneration()).thenReturn(123L);
    Iterator<Blob> blobIterator = Collections.singletonList(mockBlob).iterator();
    when(mockPage.iterateAll()).thenReturn(() -> blobIterator);
    when(mockStorage.list(anyString(), any(), any())).thenReturn(mockPage);
    when(mockStorage.delete(any(BlobId.class))).thenReturn(true);

    // Inject mock storage and bucketName into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);
    var bucketField = GoogleClient.class.getDeclaredField("bucketName");
    bucketField.setAccessible(true);
    bucketField.set(googleClient, "my-bucket");

    // Should not throw
    googleClient.deleteContent(fileName).get();
  }

  @Test
  void testUploadContent()
      throws NoSuchFieldException,
          IllegalArgumentException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException,
          IOException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    // Mock storage and writer
    Storage mockStorage = mock(Storage.class);
    WriteChannel mockWriter = mock(WriteChannel.class);

    // Inject mock storage and bucketName into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);
    var bucketField = GoogleClient.class.getDeclaredField("bucketName");
    bucketField.setAccessible(true);
    bucketField.set(googleClient, "my-bucket");

    when(mockStorage.writer(any(BlobInfo.class))).thenReturn(mockWriter);
    when(mockWriter.write(any(java.nio.ByteBuffer.class))).thenReturn(42); // return any int
    InputStream input = new java.io.ByteArrayInputStream("test".getBytes());

    // Should not throw
    googleClient.uploadContent(input, "file.txt", "text/plain").get();
  }

  @Test
  void testReadContent()
      throws NoSuchFieldException,
          IllegalArgumentException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    // Mock storage and read channel
    Storage mockStorage = mock(Storage.class);
    ReadChannel mockReadChannel = mock(ReadChannel.class);
    when(mockStorage.reader(any(com.google.cloud.storage.BlobId.class)))
        .thenReturn(mockReadChannel);

    // Inject mock storage and bucketName into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);
    var bucketField = GoogleClient.class.getDeclaredField("bucketName");
    bucketField.setAccessible(true);
    bucketField.set(googleClient, "my-bucket");

    // Should not throw
    googleClient.readContent("file.txt").get();
  }

  @Test
  void testDeleteContentDoesNotWork()
      throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    String fileName = "file.txt";

    // Mock storage and paging
    Storage mockStorage = mock(Storage.class);
    Page<Blob> mockPage = mock(Page.class);
    Blob mockBlob = mock(Blob.class);
    when(mockBlob.getName()).thenReturn(fileName);
    when(mockBlob.getGeneration()).thenReturn(123L);
    Iterator<Blob> blobIterator = Collections.singletonList(mockBlob).iterator();
    when(mockPage.iterateAll()).thenReturn(() -> blobIterator);
    when(mockStorage.list(anyString(), any(), any())).thenReturn(mockPage);
    when(mockStorage.delete(any(BlobId.class))).thenReturn(false);

    // Inject mock storage and bucketName into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);
    var bucketField = GoogleClient.class.getDeclaredField("bucketName");
    bucketField.setAccessible(true);
    bucketField.set(googleClient, "my-bucket");

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> googleClient.deleteContent(fileName).get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testUploadContentThrowsOnIOException()
      throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException, IOException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    // Mock storage and writer
    Storage mockStorage = mock(Storage.class);
    WriteChannel mockWriter = mock(WriteChannel.class);

    // Inject mock storage and bucketName into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);
    var bucketField = GoogleClient.class.getDeclaredField("bucketName");
    bucketField.setAccessible(true);
    bucketField.set(googleClient, "my-bucket");

    when(mockStorage.writer(any(BlobInfo.class))).thenReturn(mockWriter);
    // Simulate IOException on write
    doThrow(new java.io.IOException("Simulated IO failure"))
        .when(mockWriter)
        .write(any(java.nio.ByteBuffer.class));

    InputStream input = new java.io.ByteArrayInputStream("test".getBytes());

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () -> googleClient.uploadContent(input, "file.txt", "text/plain").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContentThrowsOnRuntimeException()
      throws NoSuchFieldException,
          IllegalArgumentException,
          IllegalAccessException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    // Mock storage and blob to throw RuntimeException on delete
    Storage mockStorage = mock(Storage.class);
    Blob mockBlob = mock(Blob.class);

    // Inject mock storage into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);

    when(mockStorage.get(any(String.class), any(String.class))).thenReturn(mockBlob);
    doThrow(new RuntimeException("Simulated delete failure")).when(mockBlob).delete();

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> googleClient.deleteContent("file.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testReadContentThrowsOnRuntimeException()
      throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    GoogleClient googleClient = mock(GoogleClient.class, CALLS_REAL_METHODS);

    // Mock storage and blob to throw RuntimeException on reader
    Storage mockStorage = mock(Storage.class);

    // Inject mock storage into googleClient using reflection
    var field = GoogleClient.class.getDeclaredField("storage");
    field.setAccessible(true);
    field.set(googleClient, mockStorage);
    var executorField = GoogleClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(googleClient, executor);

    // Mock blob.reader() to throw RuntimeException
    doThrow(new RuntimeException("Simulated read failure"))
        .when(mockStorage)
        .reader(any(com.google.cloud.storage.BlobId.class));

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> googleClient.readContent("file.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }
}

/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.specialized.BlobOutputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AzureClientTest {
  ExecutorService executor = Executors.newCachedThreadPool();

  @Test
  void testReadContent()
      throws NoSuchFieldException,
          SecurityException,
          IllegalArgumentException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    // Mock BlobContainerClient and BlobClient
    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlobClient);

    // Should not throw
    azureClient.readContent("file.txt").get();
  }

  @Test
  void testUploadContent()
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    // Mock BlobContainerClient and BlockBlobClient
    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlockBlobClient mockBlockBlob = mock(BlockBlobClient.class);
    BlobOutputStream mockOutputStream = mock(BlobOutputStream.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mock(BlobClient.class));
    when(mockContainer.getBlobClient(anyString()).getBlockBlobClient()).thenReturn(mockBlockBlob);
    when(mockBlockBlob.getBlobOutputStream()).thenReturn(mockOutputStream);

    InputStream mockInput = new java.io.ByteArrayInputStream("test-data".getBytes());

    // Should not throw
    azureClient.uploadContent(mockInput, "file.txt", "text/plain").get();
  }

  @Test
  void testUploadContentThrowsOnIOException()
      throws NoSuchFieldException, IllegalAccessException, IOException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    // Mock BlobContainerClient and BlockBlobClient
    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlockBlobClient mockBlockBlob = mock(BlockBlobClient.class);
    BlobOutputStream mockOutputStream = mock(BlobOutputStream.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mock(BlobClient.class));
    when(mockContainer.getBlobClient(anyString()).getBlockBlobClient()).thenReturn(mockBlockBlob);
    when(mockBlockBlob.getBlobOutputStream()).thenReturn(mockOutputStream);

    // Mock InputStream to throw IOException
    InputStream mockInput = mock(InputStream.class);
    when(mockInput.read(any(byte[].class))).thenThrow(new IOException("Simulated IO failure"));

    ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () -> azureClient.uploadContent(mockInput, "file.txt", "text/plain").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContentThrowsOnRuntimeException()
      throws NoSuchFieldException,
          SecurityException,
          IllegalArgumentException,
          IllegalAccessException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    // Mock BlobContainerClient and BlobClient
    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlobClient);

    // Mock delete to throw RuntimeException
    doThrow(new RuntimeException("Simulated delete failure")).when(mockBlobClient).delete();

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> azureClient.deleteContent("file.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  void testDeleteContent()
      throws NoSuchFieldException,
          SecurityException,
          IllegalArgumentException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    // Mock BlobContainerClient and BlobClient
    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlobClient);

    // Should not throw
    azureClient.deleteContent("file.txt").get();
  }

  @Test
  void testReadContentThrowsOnRuntimeException()
      throws NoSuchFieldException,
          SecurityException,
          IllegalArgumentException,
          IllegalAccessException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    // Mock BlobContainerClient and BlobClient
    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlobClient);

    // Mock delete to throw RuntimeException
    doThrow(new RuntimeException("Simulated read failure")).when(mockBlobClient).openInputStream();

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> azureClient.readContent("file.txt").get());
    assertInstanceOf(ObjectStoreServiceException.class, thrown.getCause());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDeleteContentByPrefix_DeletesAllMatchingBlobs()
      throws NoSuchFieldException, IllegalAccessException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    BlobContainerClient mockContainer = mock(BlobContainerClient.class);
    BlobClient mockBlobClient = mock(BlobClient.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);

    // Create mock blob items matching the prefix
    BlobItem blob1 = mock(BlobItem.class);
    when(blob1.getName()).thenReturn("tenantA/file1.txt");
    BlobItem blob2 = mock(BlobItem.class);
    when(blob2.getName()).thenReturn("tenantA/file2.txt");

    PagedIterable<BlobItem> mockPagedIterable = mock(PagedIterable.class);
    when(mockPagedIterable.stream()).thenReturn(Stream.of(blob1, blob2));
    when(mockPagedIterable.iterator()).thenReturn(Arrays.asList(blob1, blob2).iterator());
    when(mockContainer.listBlobs(any(ListBlobsOptions.class), any())).thenReturn(mockPagedIterable);
    when(mockContainer.getBlobClient(anyString())).thenReturn(mockBlobClient);

    assertDoesNotThrow(() -> azureClient.deleteContentByPrefix("tenantA/").get());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDeleteContentByPrefix_EmptyResult()
      throws NoSuchFieldException, IllegalAccessException {
    AzureClient azureClient = mock(AzureClient.class, CALLS_REAL_METHODS);

    BlobContainerClient mockContainer = mock(BlobContainerClient.class);

    var field = AzureClient.class.getDeclaredField("blobContainerClient");
    field.setAccessible(true);
    field.set(azureClient, mockContainer);
    var executorField = AzureClient.class.getDeclaredField("executor");
    executorField.setAccessible(true);
    executorField.set(azureClient, executor);

    // Empty listing
    PagedIterable<BlobItem> emptyIterable = mock(PagedIterable.class);
    when(emptyIterable.stream()).thenReturn(Stream.empty());
    when(emptyIterable.iterator()).thenReturn(Collections.emptyIterator());
    when(mockContainer.listBlobs(any(ListBlobsOptions.class), any())).thenReturn(emptyIterable);

    assertDoesNotThrow(() -> azureClient.deleteContentByPrefix("nonexistent-tenant/").get());
  }
}

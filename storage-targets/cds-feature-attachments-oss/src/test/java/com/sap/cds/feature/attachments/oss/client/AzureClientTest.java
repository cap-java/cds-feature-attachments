/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobOutputStream;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.sap.cds.feature.attachments.oss.handler.ObjectStoreServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class AzureClientTest {
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
}

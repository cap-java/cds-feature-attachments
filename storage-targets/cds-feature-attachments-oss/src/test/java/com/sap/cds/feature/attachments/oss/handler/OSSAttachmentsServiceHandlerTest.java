/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class OSSAttachmentsServiceHandlerTest {
  ExecutorService executor = Executors.newCachedThreadPool();

  @Test
  void testRestoreAttachmentCallsSetCompleted() {
    // Setup a valid AWS binding for the test
    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("host", "aws.example.com");
    creds.put("region", "us-east-1");
    creds.put("access_key_id", "test-access-key");
    creds.put("secret_access_key", "test-secret-key");
    creds.put("bucket", "test-bucket");
    when(binding.getCredentials()).thenReturn(creds);

    OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(binding, executor);
    AttachmentRestoreEventContext context = mock(AttachmentRestoreEventContext.class);
    handler.restoreAttachment(context);
    verify(context).setCompleted();
  }

  @Test
  void testCreateAttachmentCallsOsClientUploadContent()
      throws NoSuchFieldException, IllegalAccessException {
    OSClient mockOsClient = mock(OSClient.class);
    // Mock the handler, but call the real method readAttachment
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentCreateEventContext context = mock(AttachmentCreateEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    String mimeType = "text/plain";
    String fileName = "file.txt";

    MediaData mockMediaData = mock(MediaData.class);
    var mockEntity = mock(com.sap.cds.reflect.CdsEntity.class);
    when(mockEntity.getQualifiedName()).thenReturn(fileName);

    InputStream contentStream = new ByteArrayInputStream("test".getBytes());

    when(context.getAttachmentEntity()).thenReturn(mockEntity);
    when(context.getAttachmentIds()).thenReturn(java.util.Map.of("ID", contentId));
    when(context.getData()).thenReturn(mockMediaData);
    when(mockMediaData.getContent()).thenReturn(contentStream);
    when(mockMediaData.getMimeType()).thenReturn(mimeType);
    when(mockOsClient.uploadContent(any(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    when(context.getContentId()).thenReturn(contentId);

    handler.createAttachment(context);

    verify(mockOsClient).uploadContent(contentStream, contentId, mimeType);
    verify(context).setIsInternalStored(false);
    verify(context).setContentId(contentId);
    verify(context).setCompleted();
  }

  @Test
  void testReadAttachmentCallsOsClientReadContent()
      throws NoSuchFieldException, IllegalAccessException {
    OSClient mockOsClient = mock(OSClient.class);
    // Mock the handler, but call the real method readAttachment
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    MediaData mockMediaData = mock(MediaData.class);

    when(context.getContentId()).thenReturn(contentId);
    when(context.getData()).thenReturn(mockMediaData);
    when(mockOsClient.readContent(contentId))
        .thenReturn(CompletableFuture.completedFuture(new ByteArrayInputStream("test".getBytes())));

    handler.readAttachment(context);

    verify(mockOsClient).readContent(contentId);
    verify(mockMediaData).setContent(any(InputStream.class));
    verify(context).setCompleted();
  }

  @Test
  void testReadAttachmentCallsOsClientReadNullContent()
      throws NoSuchFieldException, IllegalAccessException {
    OSClient mockOsClient = mock(OSClient.class);
    // Mock the handler, but call the real method readAttachment
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    MediaData mockMediaData = mock(MediaData.class);

    when(context.getContentId()).thenReturn(contentId);
    when(context.getData()).thenReturn(mockMediaData);
    when(mockOsClient.readContent(contentId)).thenReturn(CompletableFuture.completedFuture(null));

    handler.readAttachment(context);

    verify(mockOsClient).readContent(contentId);
    verify(context).setCompleted();
  }

  @Test
  void testMarkAttachmentAsDeletedCallsOsClientDeleteContent()
      throws NoSuchFieldException, IllegalAccessException {
    OSClient mockOsClient = mock(OSClient.class);
    // Mock the handler, but call the real method readAttachment
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentMarkAsDeletedEventContext context = mock(AttachmentMarkAsDeletedEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    when(context.getContentId()).thenReturn(contentId);
    when(mockOsClient.deleteContent(contentId)).thenReturn(CompletableFuture.completedFuture(null));

    handler.markAttachmentAsDeleted(context);

    verify(mockOsClient).deleteContent(contentId);
    verify(context).setCompleted();
  }

  @Test
  void testConstructorHandlesInvalidBase64EncodedPrivateKeyData() {
    // Arrange: ServiceBinding with invalid base64EncodedPrivateKeyData (not valid base64)
    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("base64EncodedPrivateKeyData", "not-a-valid-base64-string");
    when(binding.getCredentials()).thenReturn(creds);

    assertThrows(
        ObjectStoreServiceException.class,
        () -> {
          new OSSAttachmentsServiceHandler(binding, executor);
        });
  }

  @Test
  void testConstructorHandlesValidBase64ButNoGoogleOrGcp() {
    String plain = "this is just a dummy string without keywords";
    String base64 =
        Base64.getEncoder().encodeToString(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("base64EncodedPrivateKeyData", base64);
    when(binding.getCredentials()).thenReturn(creds);

    assertThrows(
        ObjectStoreServiceException.class,
        () -> {
          new OSSAttachmentsServiceHandler(binding, executor);
        });
  }

  @Test
  void testConstructorHandlesInValidBase64() {
    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("base64EncodedPrivateKeyData", "this is just a dummy string without keywords");
    when(binding.getCredentials()).thenReturn(creds);

    assertThrows(
        ObjectStoreServiceException.class,
        () -> {
          new OSSAttachmentsServiceHandler(binding, executor);
        });
  }

  @Test
  void testConstructorHandlesNoValidObjectStoreService() {
    // Arrange: ServiceBinding with no valid object store credentials
    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    // No host, container_uri, or base64EncodedPrivateKeyData
    creds.put("someOtherField", "someValue");
    when(binding.getCredentials()).thenReturn(creds);

    assertThrows(
        ObjectStoreServiceException.class,
        () -> {
          new OSSAttachmentsServiceHandler(binding, executor);
        });
  }

  @Test
  void testCreateAttachmentHandlesInterruptedException()
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    OSClient mockOsClient = mock(OSClient.class);
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentCreateEventContext context = mock(AttachmentCreateEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    String mimeType = "text/plain";
    String fileName = "file.txt";

    MediaData mockMediaData = mock(MediaData.class);
    var mockEntity = mock(CdsEntity.class);
    when(mockEntity.getQualifiedName()).thenReturn(fileName);

    InputStream contentStream = new ByteArrayInputStream("test".getBytes());

    when(context.getAttachmentEntity()).thenReturn(mockEntity);
    when(context.getAttachmentIds()).thenReturn(java.util.Map.of("ID", contentId));
    when(context.getData()).thenReturn(mockMediaData);
    when(mockMediaData.getContent()).thenReturn(contentStream);
    when(mockMediaData.getMimeType()).thenReturn(mimeType);
    when(mockMediaData.getFileName()).thenReturn(fileName);

    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.uploadContent(any(), anyString(), anyString())).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("Thread interrupted"));

    assertThrows(ServiceException.class, () -> handler.createAttachment(context));
    verify(context).setCompleted();
  }

  @Test
  void testMarkAttachmentAsDeletedHandlesInterruptedException()
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    OSClient mockOsClient = mock(OSClient.class);
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentMarkAsDeletedEventContext context = mock(AttachmentMarkAsDeletedEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    when(context.getContentId()).thenReturn(contentId);

    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.deleteContent(contentId)).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("Thread interrupted"));

    assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context));
    verify(context).setCompleted();
  }

  @Test
  void testReadAttachmentHandlesInterruptedException()
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    OSClient mockOsClient = mock(OSClient.class);
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    MediaData mockMediaData = mock(MediaData.class);

    when(context.getContentId()).thenReturn(contentId);
    when(context.getData()).thenReturn(mockMediaData);

    CompletableFuture<InputStream> future = mock(CompletableFuture.class);
    when(mockOsClient.readContent(contentId)).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("Thread interrupted"));

    assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    verify(context).setCompleted();
  }
}

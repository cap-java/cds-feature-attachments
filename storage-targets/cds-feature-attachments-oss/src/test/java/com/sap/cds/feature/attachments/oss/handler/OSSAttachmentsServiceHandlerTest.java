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

    assertThrows(ServiceException.class, () -> handler.readAttachment(context));

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
        () -> new OSSAttachmentsServiceHandler(binding, executor));
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
        () -> new OSSAttachmentsServiceHandler(binding, executor));
  }

  @Test
  void testConstructorHandlesInValidBase64() {
    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("base64EncodedPrivateKeyData", "this is just a dummy string without keywords");
    when(binding.getCredentials()).thenReturn(creds);

    assertThrows(
        ObjectStoreServiceException.class,
        () -> new OSSAttachmentsServiceHandler(binding, executor));
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
        () -> new OSSAttachmentsServiceHandler(binding, executor));
  }

  // Helper method to setup common mocks for createAttachment exception tests
  private AttachmentCreateEventContext setupCreateAttachmentContext(
      OSClient mockOsClient, OSSAttachmentsServiceHandler handler, Exception exceptionToThrow)
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    AttachmentCreateEventContext context = mock(AttachmentCreateEventContext.class);
    MediaData mockMediaData = mock(MediaData.class);
    CdsEntity mockEntity = mock(CdsEntity.class);
    HashMap<String, Object> attachmentIds = new HashMap<>();
    attachmentIds.put("ID", "test-id");

    when(context.getAttachmentIds()).thenReturn(attachmentIds);
    when(context.getData()).thenReturn(mockMediaData);
    when(context.getAttachmentEntity()).thenReturn(mockEntity);
    when(mockEntity.getQualifiedName()).thenReturn("TestEntity");
    when(mockMediaData.getFileName()).thenReturn("test.txt");
    when(mockMediaData.getContent()).thenReturn(new ByteArrayInputStream("test".getBytes()));
    when(mockMediaData.getMimeType()).thenReturn("text/plain");

    @SuppressWarnings("unchecked")
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.uploadContent(any(InputStream.class), anyString(), anyString()))
        .thenReturn(future);
    when(future.get()).thenThrow(exceptionToThrow);

    return context;
  }

  @Test
  void testCreateAttachmentExceptionHandling()
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    OSClient mockOsClient = mock(OSClient.class);
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);

    // Test InterruptedException
    AttachmentCreateEventContext context1 =
        setupCreateAttachmentContext(
            mockOsClient, handler, new InterruptedException("Thread interrupted"));
    assertThrows(ServiceException.class, () -> handler.createAttachment(context1));
    verify(context1).setCompleted();

    // Test ObjectStoreServiceException
    AttachmentCreateEventContext context2 =
        setupCreateAttachmentContext(
            mockOsClient, handler, new ObjectStoreServiceException("Upload failed"));
    assertThrows(ServiceException.class, () -> handler.createAttachment(context2));
    verify(context2).setCompleted();

    // Test ExecutionException
    AttachmentCreateEventContext context3 =
        setupCreateAttachmentContext(
            mockOsClient, handler, new ExecutionException("Upload failed", new RuntimeException()));
    assertThrows(ServiceException.class, () -> handler.createAttachment(context3));
    verify(context3).setCompleted();
  }

  // Helper method to setup common mocks for markAttachmentAsDeleted exception tests
  private AttachmentMarkAsDeletedEventContext setupMarkAsDeletedContext(
      OSClient mockOsClient, OSSAttachmentsServiceHandler handler, Exception exceptionToThrow)
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {

    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    AttachmentMarkAsDeletedEventContext context = mock(AttachmentMarkAsDeletedEventContext.class);
    String contentId = "test-content-id";

    when(context.getContentId()).thenReturn(contentId);

    @SuppressWarnings("unchecked")
    CompletableFuture<Void> future = mock(CompletableFuture.class);
    when(mockOsClient.deleteContent(contentId)).thenReturn(future);
    when(future.get()).thenThrow(exceptionToThrow);

    return context;
  }

  @Test
  void testMarkAttachmentAsDeletedExceptionHandling()
      throws NoSuchFieldException,
          IllegalAccessException,
          InterruptedException,
          ExecutionException {
    OSClient mockOsClient = mock(OSClient.class);
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);

    // Test InterruptedException
    AttachmentMarkAsDeletedEventContext context1 =
        setupMarkAsDeletedContext(
            mockOsClient, handler, new InterruptedException("Thread interrupted"));
    assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context1));
    verify(context1).setCompleted();

    // Test ObjectStoreServiceException
    AttachmentMarkAsDeletedEventContext context2 =
        setupMarkAsDeletedContext(
            mockOsClient, handler, new ObjectStoreServiceException("Delete failed"));
    assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context2));
    verify(context2).setCompleted();

    // Test ExecutionException
    AttachmentMarkAsDeletedEventContext context3 =
        setupMarkAsDeletedContext(
            mockOsClient, handler, new ExecutionException("Delete failed", new RuntimeException()));
    assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context3));
    verify(context3).setCompleted();
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

    @SuppressWarnings("unchecked")
    CompletableFuture<InputStream> future = mock(CompletableFuture.class);
    when(mockOsClient.readContent(contentId)).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException("Thread interrupted"));

    assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    verify(context).setCompleted();
  }
}

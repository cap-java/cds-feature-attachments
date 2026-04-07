/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.oss.client.OSClientFactory;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.request.ModifiableUserInfo;
import com.sap.cds.services.request.UserInfo;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OSSAttachmentsServiceHandlerTest {

  private static final ExecutorService executor = Executors.newCachedThreadPool();

  private OSClient mockOsClient;
  private OSSAttachmentsServiceHandler handler;

  private static ServiceBinding createAwsBinding() {
    ServiceBinding binding = mock(ServiceBinding.class);
    HashMap<String, Object> creds = new HashMap<>();
    creds.put("host", "aws.example.com");
    creds.put("region", "us-east-1");
    creds.put("access_key_id", "test-access-key");
    creds.put("secret_access_key", "test-secret-key");
    creds.put("bucket", "test-bucket");
    when(binding.getCredentials()).thenReturn(creds);
    return binding;
  }

  private static CdsEntity stubEntity(String name) {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn(name);
    return entity;
  }

  /**
   * Creates a real {@link AttachmentCreateEventContext} populated with the given values. The only
   * mock used is CdsEntity (a model-level concept not creatable without a full model).
   */
  private static AttachmentCreateEventContext createContext(
      String contentId, String mimeType, String fileName, byte[] content) {
    var ctx = AttachmentCreateEventContext.create();
    ctx.setData(MediaData.create());
    ctx.getData().setContent(new ByteArrayInputStream(content));
    ctx.getData().setMimeType(mimeType);
    ctx.getData().setFileName(fileName);
    ctx.setAttachmentIds(Map.of(Attachments.ID, contentId));
    ctx.setAttachmentEntity(stubEntity("TestEntity"));
    return ctx;
  }

  private static UserInfo userInfoWithTenant(String tenant) {
    ModifiableUserInfo userInfo = UserInfo.create();
    userInfo.setTenant(tenant);
    return userInfo;
  }

  @Nested
  class FactoryTests {

    @Test
    void testFactoryHandlesInvalidBase64EncodedPrivateKeyData() {
      ServiceBinding binding = mock(ServiceBinding.class);
      HashMap<String, Object> creds = new HashMap<>();
      creds.put("base64EncodedPrivateKeyData", "not-a-valid-base64-string");
      when(binding.getCredentials()).thenReturn(creds);

      assertThrows(
          ObjectStoreServiceException.class,
          () -> OSClientFactory.create(binding, executor));
    }

    @Test
    void testFactoryHandlesValidBase64ButNoGoogleOrGcp() {
      String plain = "this is just a dummy string without keywords";
      String base64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

      ServiceBinding binding = mock(ServiceBinding.class);
      HashMap<String, Object> creds = new HashMap<>();
      creds.put("base64EncodedPrivateKeyData", base64);
      when(binding.getCredentials()).thenReturn(creds);

      assertThrows(
          ObjectStoreServiceException.class,
          () -> OSClientFactory.create(binding, executor));
    }

    @Test
    void testFactoryHandlesInValidBase64() {
      ServiceBinding binding = mock(ServiceBinding.class);
      HashMap<String, Object> creds = new HashMap<>();
      creds.put("base64EncodedPrivateKeyData", "this is just a dummy string without keywords");
      when(binding.getCredentials()).thenReturn(creds);

      assertThrows(
          ObjectStoreServiceException.class,
          () -> OSClientFactory.create(binding, executor));
    }

    @Test
    void testFactoryHandlesNoValidObjectStoreService() {
      ServiceBinding binding = mock(ServiceBinding.class);
      HashMap<String, Object> creds = new HashMap<>();
      creds.put("someOtherField", "someValue");
      when(binding.getCredentials()).thenReturn(creds);

      assertThrows(
          ObjectStoreServiceException.class,
          () -> OSClientFactory.create(binding, executor));
    }
  }

  @Nested
  class SingleTenantOperations {

    @BeforeEach
    void setup() {
      mockOsClient = mock(OSClient.class);
      handler = new OSSAttachmentsServiceHandler(mockOsClient, false, null);
    }

    @Test
    void testRestoreAttachmentCallsSetCompleted() {
      var context = AttachmentRestoreEventContext.create();
      context.setRestoreTimestamp(Instant.now());

      handler.restoreAttachment(context);

      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testCreateAttachmentUploadsContent() {
      when(mockOsClient.uploadContent(any(), anyString(), anyString()))
          .thenReturn(CompletableFuture.completedFuture(null));

      var context = createContext("doc123", "text/plain", "file.txt", "test".getBytes());

      handler.createAttachment(context);

      verify(mockOsClient).uploadContent(any(InputStream.class), eq("doc123"), eq("text/plain"));
      assertThat(context.getIsInternalStored()).isFalse();
      assertThat(context.getContentId()).isEqualTo("doc123");
      assertThat(context.getData().getStatus()).isEqualTo(StatusCode.SCANNING);
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testReadAttachmentReadsContent() {
      when(mockOsClient.readContent("doc123"))
          .thenReturn(
              CompletableFuture.completedFuture(new ByteArrayInputStream("test".getBytes())));

      var context = AttachmentReadEventContext.create();
      context.setContentId("doc123");
      context.setData(MediaData.create());

      handler.readAttachment(context);

      verify(mockOsClient).readContent("doc123");
      assertThat(context.getData().getContent()).isNotNull();
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testReadAttachmentWithNullContentThrows() {
      when(mockOsClient.readContent("doc123")).thenReturn(CompletableFuture.completedFuture(null));

      var context = AttachmentReadEventContext.create();
      context.setContentId("doc123");
      context.setData(MediaData.create());

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testMarkAttachmentAsDeletedDeletesContent() {
      when(mockOsClient.deleteContent("doc123"))
          .thenReturn(CompletableFuture.completedFuture(null));

      var context = AttachmentMarkAsDeletedEventContext.create();
      context.setContentId("doc123");

      handler.markAttachmentAsDeleted(context);

      verify(mockOsClient).deleteContent("doc123");
      assertThat(context.isCompleted()).isTrue();
    }
  }

  @Nested
  class ExceptionHandling {

    @BeforeEach
    void setup() {
      mockOsClient = mock(OSClient.class);
      handler = new OSSAttachmentsServiceHandler(mockOsClient, false, null);
    }

    @Test
    void testCreateAttachmentHandlesInterruptedException() throws Exception {
      var context = createContextForUploadException(new InterruptedException("Thread interrupted"));
      assertThrows(ServiceException.class, () -> handler.createAttachment(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testCreateAttachmentHandlesObjectStoreServiceException() throws Exception {
      var context =
          createContextForUploadException(new ObjectStoreServiceException("Upload failed"));
      assertThrows(ServiceException.class, () -> handler.createAttachment(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testCreateAttachmentHandlesExecutionException() throws Exception {
      var context =
          createContextForUploadException(
              new ExecutionException("Upload failed", new RuntimeException()));
      assertThrows(ServiceException.class, () -> handler.createAttachment(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testMarkAsDeletedHandlesInterruptedException() throws Exception {
      var context = createContextForDeleteException(new InterruptedException("Thread interrupted"));
      assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testMarkAsDeletedHandlesObjectStoreServiceException() throws Exception {
      var context =
          createContextForDeleteException(new ObjectStoreServiceException("Delete failed"));
      assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testMarkAsDeletedHandlesExecutionException() throws Exception {
      var context =
          createContextForDeleteException(
              new ExecutionException("Delete failed", new RuntimeException()));
      assertThrows(ServiceException.class, () -> handler.markAttachmentAsDeleted(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testReadAttachmentHandlesInterruptedException() throws Exception {
      var context = createContextForReadException(new InterruptedException("Thread interrupted"));
      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
      assertThat(context.isCompleted()).isTrue();
    }

    @Test
    void testReadAttachmentHandlesExecutionException() throws Exception {
      var context =
          createContextForReadException(new ExecutionException("failed", new RuntimeException()));
      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
      assertThat(context.isCompleted()).isTrue();
    }

    private AttachmentCreateEventContext createContextForUploadException(Exception exception)
        throws Exception {
      @SuppressWarnings("unchecked")
      CompletableFuture<Void> future = mock(CompletableFuture.class);
      when(mockOsClient.uploadContent(any(InputStream.class), anyString(), anyString()))
          .thenReturn(future);
      when(future.get()).thenThrow(exception);

      return createContext("test-id", "text/plain", "test.txt", "test".getBytes());
    }

    private AttachmentMarkAsDeletedEventContext createContextForDeleteException(Exception exception)
        throws Exception {
      @SuppressWarnings("unchecked")
      CompletableFuture<Void> future = mock(CompletableFuture.class);
      when(mockOsClient.deleteContent("test-content-id")).thenReturn(future);
      when(future.get()).thenThrow(exception);

      var context = AttachmentMarkAsDeletedEventContext.create();
      context.setContentId("test-content-id");
      return context;
    }

    private AttachmentReadEventContext createContextForReadException(Exception exception)
        throws Exception {
      @SuppressWarnings("unchecked")
      CompletableFuture<InputStream> future = mock(CompletableFuture.class);
      when(mockOsClient.readContent("doc123")).thenReturn(future);
      when(future.get()).thenThrow(exception);

      var context = AttachmentReadEventContext.create();
      context.setContentId("doc123");
      context.setData(MediaData.create());
      return context;
    }
  }

  @Nested
  class MultitenancyTests {

    @BeforeEach
    void setup() {
      mockOsClient = mock(OSClient.class);
      handler = new OSSAttachmentsServiceHandler(mockOsClient, true, "shared");
    }

    @Test
    void testCreateAttachmentWithMultitenancyBuildsObjectKey() {
      when(mockOsClient.uploadContent(any(), anyString(), anyString()))
          .thenReturn(CompletableFuture.completedFuture(null));

      // For multitenancy, getUserInfo() requires a RequestContext, so we mock
      // the event context to provide tenant info
      CdsEntity entity = stubEntity("TestEntity");
      UserInfo userInfo = userInfoWithTenant("myTenant");
      MediaData data = MediaData.create();
      data.setContent(new ByteArrayInputStream("test".getBytes()));
      data.setMimeType("text/plain");
      data.setFileName("file.txt");

      AttachmentCreateEventContext context = mock(AttachmentCreateEventContext.class);
      when(context.getAttachmentEntity()).thenReturn(entity);
      when(context.getAttachmentIds()).thenReturn(Map.of(Attachments.ID, "content123"));
      when(context.getData()).thenReturn(data);
      when(context.getUserInfo()).thenReturn(userInfo);

      handler.createAttachment(context);

      verify(mockOsClient).uploadContent(any(), eq("myTenant/content123"), anyString());
    }

    @Test
    void testReadAttachmentWithMultitenancyBuildsObjectKey() {
      when(mockOsClient.readContent("myTenant/content123"))
          .thenReturn(
              CompletableFuture.completedFuture(new ByteArrayInputStream("test".getBytes())));

      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getData()).thenReturn(MediaData.create());
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("myTenant"));

      handler.readAttachment(context);

      verify(mockOsClient).readContent("myTenant/content123");
    }

    @Test
    void testMarkAsDeletedWithMultitenancyBuildsObjectKey() {
      when(mockOsClient.deleteContent("myTenant/content123"))
          .thenReturn(CompletableFuture.completedFuture(null));

      AttachmentMarkAsDeletedEventContext context = mock(AttachmentMarkAsDeletedEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("myTenant"));

      handler.markAttachmentAsDeleted(context);

      verify(mockOsClient).deleteContent("myTenant/content123");
    }

    @Test
    void testMultitenancyWithNullTenantThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant(null));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateTenantIdWithSlashThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("tenant/evil"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateTenantIdWithBackslashThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("tenant\\evil"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateTenantIdWithDotsThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("..evil"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateEmptyTenantIdThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content123");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant(""));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateContentIdWithSlashThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content/evil");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("validTenant"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateContentIdWithNullThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn(null);
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("validTenant"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateContentIdWithBackslashThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("content\\evil");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("validTenant"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateContentIdWithDotsThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("..evil");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("validTenant"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }

    @Test
    void testValidateEmptyContentIdThrows() {
      AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
      when(context.getContentId()).thenReturn("");
      when(context.getUserInfo()).thenReturn(userInfoWithTenant("validTenant"));

      assertThrows(ServiceException.class, () -> handler.readAttachment(context));
    }
  }
}

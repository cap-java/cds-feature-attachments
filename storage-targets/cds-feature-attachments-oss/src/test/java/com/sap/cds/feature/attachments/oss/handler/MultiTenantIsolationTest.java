/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.services.ServiceException;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.services.request.UserInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration-level test that verifies complete tenant isolation in shared mode. Uses a tracking
 * mock OSClient to record all operations and verify that Tenant A's operations never touch Tenant
 * B's key space.
 */
class MultiTenantIsolationTest {

  /** Tracks all upload, read, and delete keys passed to the OSClient. */
  private final List<String> uploadedKeys = new ArrayList<>();

  private final List<String> readKeys = new ArrayList<>();
  private final List<String> deletedKeys = new ArrayList<>();
  private final Map<String, byte[]> storage = new HashMap<>();

  private OSClient trackingClient;
  private OSSAttachmentsServiceHandler handler;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    uploadedKeys.clear();
    readKeys.clear();
    deletedKeys.clear();
    storage.clear();

    // Create a tracking OSClient that records all operations
    trackingClient = mock(OSClient.class);

    when(trackingClient.uploadContent(any(InputStream.class), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              InputStream content = invocation.getArgument(0);
              String key = invocation.getArgument(1);
              uploadedKeys.add(key);
              storage.put(key, content.readAllBytes());
              return CompletableFuture.completedFuture(null);
            });

    when(trackingClient.readContent(anyString()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              readKeys.add(key);
              byte[] data = storage.get(key);
              if (data != null) {
                return CompletableFuture.completedFuture(
                    (InputStream) new ByteArrayInputStream(data));
              }
              return CompletableFuture.completedFuture(null);
            });

    when(trackingClient.deleteContent(anyString()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              deletedKeys.add(key);
              storage.remove(key);
              return CompletableFuture.completedFuture(null);
            });

    // Create handler with multitenancy enabled in shared mode
    handler = mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);

    var osClientField = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    osClientField.setAccessible(true);
    osClientField.set(handler, trackingClient);

    var mtEnabledField =
        OSSAttachmentsServiceHandler.class.getDeclaredField("multitenancyEnabled");
    mtEnabledField.setAccessible(true);
    mtEnabledField.set(handler, true);

    var kindField = OSSAttachmentsServiceHandler.class.getDeclaredField("objectStoreKind");
    kindField.setAccessible(true);
    kindField.set(handler, "shared");
  }

  @Test
  void testTwoTenants_Upload_Read_Delete_CompleteIsolation() {
    String tenantA = "tenantA";
    String tenantB = "tenantB";
    String contentIdA = "content-id-A";
    String contentIdB = "content-id-B";

    // Tenant A uploads
    simulateCreate(tenantA, contentIdA, "Data from Tenant A");
    // Tenant B uploads
    simulateCreate(tenantB, contentIdB, "Data from Tenant B");

    // Verify keys are prefixed correctly
    assertEquals(2, uploadedKeys.size());
    assertTrue(uploadedKeys.contains(tenantA + "/" + contentIdA));
    assertTrue(uploadedKeys.contains(tenantB + "/" + contentIdB));

    // Verify no cross-tenant key overlap
    assertFalse(uploadedKeys.stream().anyMatch(k -> k.startsWith(tenantA) && k.contains(contentIdB)));
    assertFalse(uploadedKeys.stream().anyMatch(k -> k.startsWith(tenantB) && k.contains(contentIdA)));

    // Tenant A reads its own content
    simulateRead(tenantA, contentIdA);
    assertEquals(tenantA + "/" + contentIdA, readKeys.get(0));

    // Tenant B reads its own content
    simulateRead(tenantB, contentIdB);
    assertEquals(tenantB + "/" + contentIdB, readKeys.get(1));

    // Tenant A deletes its content
    simulateDelete(tenantA, contentIdA);
    assertEquals(tenantA + "/" + contentIdA, deletedKeys.get(0));

    // Tenant B's content is still in storage
    assertTrue(storage.containsKey(tenantB + "/" + contentIdB));
    assertFalse(storage.containsKey(tenantA + "/" + contentIdA));

    // Tenant B deletes its content
    simulateDelete(tenantB, contentIdB);
    assertEquals(tenantB + "/" + contentIdB, deletedKeys.get(1));

    // Both are gone
    assertTrue(storage.isEmpty());
  }

  @Test
  void testTenantA_CannotAccess_TenantB_Data() {
    String tenantA = "tenantA";
    String tenantB = "tenantB";
    String sharedContentId = "same-content-id";

    // Both tenants upload with the same contentId (UUID)
    simulateCreate(tenantA, sharedContentId, "Tenant A secret");
    simulateCreate(tenantB, sharedContentId, "Tenant B secret");

    // Keys should be different due to tenant prefix
    assertEquals(2, uploadedKeys.size());
    assertEquals(tenantA + "/" + sharedContentId, uploadedKeys.get(0));
    assertEquals(tenantB + "/" + sharedContentId, uploadedKeys.get(1));

    // Storage should have 2 separate entries
    assertEquals(2, storage.size());

    // Reading as tenant A only returns tenant A's data
    simulateRead(tenantA, sharedContentId);
    assertEquals(tenantA + "/" + sharedContentId, readKeys.get(0));

    // The read key never touches tenant B's namespace
    assertFalse(readKeys.get(0).startsWith(tenantB));
  }

  @Test
  void testNullTenant_ThrowsInSharedMTMode() {
    String contentId = "content-no-tenant";

    // In MT shared mode, null tenant must throw (H-1 security fix)
    assertThrows(
        ServiceException.class, () -> simulateCreate(null, contentId, "No tenant data"));

    // No upload should have occurred
    assertEquals(0, uploadedKeys.size());
  }

  private void simulateCreate(String tenant, String contentId, String content) {
    AttachmentCreateEventContext context = mock(AttachmentCreateEventContext.class);
    MediaData mockMediaData = mock(MediaData.class);
    var mockEntity = mock(com.sap.cds.reflect.CdsEntity.class);
    UserInfo userInfo = mock(UserInfo.class);

    when(userInfo.getTenant()).thenReturn(tenant);
    when(context.getUserInfo()).thenReturn(userInfo);
    when(context.getAttachmentEntity()).thenReturn(mockEntity);
    when(mockEntity.getQualifiedName()).thenReturn("TestEntity");
    when(context.getAttachmentIds()).thenReturn(Map.of(Attachments.ID, contentId));
    when(context.getData()).thenReturn(mockMediaData);
    when(mockMediaData.getContent()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    when(mockMediaData.getMimeType()).thenReturn("text/plain");

    handler.createAttachment(context);
  }

  private void simulateRead(String tenant, String contentId) {
    AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);
    MediaData mockMediaData = mock(MediaData.class);
    UserInfo userInfo = mock(UserInfo.class);

    when(userInfo.getTenant()).thenReturn(tenant);
    when(context.getUserInfo()).thenReturn(userInfo);
    when(context.getContentId()).thenReturn(contentId);
    when(context.getData()).thenReturn(mockMediaData);

    handler.readAttachment(context);
  }

  private void simulateDelete(String tenant, String contentId) {
    AttachmentMarkAsDeletedEventContext context = mock(AttachmentMarkAsDeletedEventContext.class);
    UserInfo userInfo = mock(UserInfo.class);

    when(userInfo.getTenant()).thenReturn(tenant);
    when(context.getUserInfo()).thenReturn(userInfo);
    when(context.getContentId()).thenReturn(contentId);

    handler.markAttachmentAsDeleted(context);
  }
}

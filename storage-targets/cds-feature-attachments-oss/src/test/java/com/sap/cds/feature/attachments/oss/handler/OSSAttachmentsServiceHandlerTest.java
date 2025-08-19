package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class OSSAttachmentsServiceHandlerTest {
    @Test
    void testRestoreAttachmentCallsSetCompleted() {
        ServiceBinding binding = mock(ServiceBinding.class);
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(binding));
        AttachmentRestoreEventContext context = mock(AttachmentRestoreEventContext.class);
        handler.restoreAttachment(context);
        verify(context).setCompleted();
    }

    @Test
    void testCreateAttachmentCallsOsClientUploadContent() throws NoSuchFieldException, IllegalAccessException, InterruptedException, ExecutionException {
        OSClient mockOsClient = mock(OSClient.class);
        // Mock the handler, but call the real method readAttachment
        OSSAttachmentsServiceHandler handler = mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
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
        when(mockOsClient.uploadContent(any(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        when(context.getContentId()).thenReturn(contentId);

        handler.createAttachment(context);

        verify(mockOsClient).uploadContent(contentStream, contentId, mimeType);
        verify(context).setIsInternalStored(false);
        verify(context).setContentId(contentId);
        verify(context).setCompleted();
    }

    @Test
    void testReadAttachmentCallsOsClientReadContent() throws NoSuchFieldException, IllegalAccessException, InterruptedException, ExecutionException {
        OSClient mockOsClient = mock(OSClient.class);
        // Mock the handler, but call the real method readAttachment
        OSSAttachmentsServiceHandler handler = mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
        AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);

        var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
        field.setAccessible(true);
        field.set(handler, mockOsClient);

        String contentId = "doc123";
        MediaData mockMediaData = mock(MediaData.class);

        when(context.getContentId()).thenReturn(contentId);
        when(context.getData()).thenReturn(mockMediaData);
        when(mockOsClient.readContent(contentId)).thenReturn(CompletableFuture.completedFuture(new ByteArrayInputStream("test".getBytes())));

        handler.readAttachment(context);

        verify(mockOsClient).readContent(contentId);
        verify(mockMediaData).setContent(any(InputStream.class));
        verify(context).setCompleted();
    }

    @Test
    void testReadAttachmentCallsOsClientReadNullContent() throws NoSuchFieldException, IllegalAccessException, InterruptedException, ExecutionException {
        OSClient mockOsClient = mock(OSClient.class);
        // Mock the handler, but call the real method readAttachment
        OSSAttachmentsServiceHandler handler = mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
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
    void testMarkAttachmentAsDeletedCallsOsClientDeleteContent() throws NoSuchFieldException, IllegalAccessException, InterruptedException, ExecutionException {
        OSClient mockOsClient = mock(OSClient.class);
        // Mock the handler, but call the real method readAttachment
        OSSAttachmentsServiceHandler handler = mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
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

        // Act: Should not throw, but should use MockOSClient as fallback
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(binding));
        // Optionally, check that the handler uses MockOSClient
        try {
            var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
            field.setAccessible(true);
            Object osClient = field.get(handler);
            assertTrue(osClient.getClass().getSimpleName().contains("MockOSClient"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConstructorHandlesValidBase64ButNoGoogleOrGcp() {
        String plain = "this is just a dummy string without keywords";
        String base64 = Base64.getEncoder().encodeToString(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        testConstructorHandlesBase64ResultingInMockClient(base64);
    }

    @Test
    void testConstructorHandlesInValidBase64() {
        testConstructorHandlesBase64ResultingInMockClient("this is just a dummy string without keywords");
    }

    void testConstructorHandlesBase64ResultingInMockClient(String base64) {
        ServiceBinding binding = mock(ServiceBinding.class);
        HashMap<String, Object> creds = new HashMap<>();
        creds.put("base64EncodedPrivateKeyData", base64);
        when(binding.getCredentials()).thenReturn(creds);

        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(binding));
        try {
            var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
            field.setAccessible(true);
            Object osClient = field.get(handler);
            assertTrue(osClient.getClass().getSimpleName().contains("MockOSClient"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

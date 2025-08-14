package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class OSSAttachmentsServiceHandlerTest {
    @Test
    void testRestoreAttachmentCallsSetCompleted() {
        // Arrange
        ServiceBinding binding = mock(ServiceBinding.class);
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.of(binding));
        AttachmentRestoreEventContext context = mock(AttachmentRestoreEventContext.class);

        // Act
        handler.restoreAttachment(context);

        // Assert
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
        // Arrange: ServiceBinding with valid base64EncodedPrivateKeyData, but not containing "google" or "gcp"
        ServiceBinding binding = mock(ServiceBinding.class);
        HashMap<String, Object> creds = new HashMap<>();
        creds.put("base64EncodedPrivateKeyData", base64);
        when(binding.getCredentials()).thenReturn(creds);

        // Act: Should use MockOSClient as fallback
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

/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public class OSSAttachmentsServiceHandlerTestUtils {

  // This methods tests the complete flow of creating, reading, and deleting an attachment
  // for all OS clients. It uses a mock ServiceBinding to simulate the attachment service.
  public static void testCreateReadDeleteAttachmentFlow(
      ServiceBinding binding, ExecutorService executor) {
    // Create test file to upload, read and delete
    String testFileName = "testFileName-" + System.currentTimeMillis() + ".txt";
    String testFileContent = "test";

    OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(binding, executor);

    // Create an AttachmentCreateEventContext with mocked data - to upload a test attachment
    MediaData createMediaData = mock(MediaData.class);
    when(createMediaData.getMimeType()).thenReturn("text/plain");
    InputStream content = new ByteArrayInputStream(testFileContent.getBytes());
    when(createMediaData.getContent()).thenReturn(content);

    CdsEntity attachmentEntity = mock(CdsEntity.class);
    when(attachmentEntity.getQualifiedName()).thenReturn(testFileName);

    AttachmentCreateEventContext createContext = mock(AttachmentCreateEventContext.class);
    when(createContext.getData()).thenReturn(createMediaData);
    when(createContext.getAttachmentEntity()).thenReturn(attachmentEntity);
    when(createContext.getAttachmentIds())
        .thenReturn(
            new HashMap<>() {
              {
                put(Attachments.ID, testFileName);
              }
            });
    doNothing().when(createContext).setCompleted();

    handler.createAttachment(createContext);
    // Verify that the function setCompleted was called
    verify(createContext).setCompleted();

    // Now read attachment
    MediaData readMediaData = mock(MediaData.class);
    // When calling readAttachment, we modify the readMetaData by calling setContent.
    // To check if these functions are called correctly, we use Mockito's doAnswer to capture the
    // arguments passed to these methods.
    doAnswer(
            invocation -> {
              InputStream receivedInputStream = invocation.getArgument(0);
              assertEquals(testFileContent, new String(receivedInputStream.readAllBytes()));
              return null;
            })
        .when(readMediaData)
        .setContent(any());

    AttachmentReadEventContext readContext = mock(AttachmentReadEventContext.class);
    when(readContext.getContentId()).thenReturn(testFileName);
    when(readContext.getData()).thenReturn(readMediaData);
    doNothing().when(readContext).setCompleted();

    handler.readAttachment(readContext);
    // Verify that the function setCompleted was called
    verify(readContext).setCompleted();

    // Delete attachment
    AttachmentMarkAsDeletedEventContext deleteContext =
        mock(AttachmentMarkAsDeletedEventContext.class);
    when(deleteContext.getContentId()).thenReturn(testFileName);
    doNothing().when(readContext).setCompleted();

    handler.markAttachmentAsDeleted(deleteContext);
    // Verify that the function setCompleted was called
    verify(deleteContext).setCompleted();

    // Try to read again, this will throw
    assertThrows(IOException.class, () -> handler.readAttachment(readContext));
    verify(deleteContext).setCompleted();
  }

  // Helper to access private static osClient
  public static OSClient getOsClient(OSSAttachmentsServiceHandler handler)
      throws NoSuchFieldException, IllegalAccessException {
    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    return (OSClient) field.get(handler);
  }
}

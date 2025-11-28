/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OSSAttachmentsServiceHandlerStreamLifecycleTest {

  /**
   * Regression test: verifies that the OSS handler does NOT close the InputStream before the
   * framework/consumer reads it (API, non-OData path).
   *
   * This would fail against the previous implementation that used try-with-resources around the
   * stream because our ClosingAwareInputStream throws on reads after close.
   */
  @Test
  void testReadAttachmentDoesNotCloseStreamBeforeConsumption() throws Exception {
    // Arrange
    OSClient mockOsClient = mock(OSClient.class);
    // Create a partial mock that calls real methods (constructor is not invoked)
    OSSAttachmentsServiceHandler handler =
        mock(OSSAttachmentsServiceHandler.class, CALLS_REAL_METHODS);
    AttachmentReadEventContext context = mock(AttachmentReadEventContext.class);

    // Inject mock OS client
    var field = OSSAttachmentsServiceHandler.class.getDeclaredField("osClient");
    field.setAccessible(true);
    field.set(handler, mockOsClient);

    String contentId = "doc123";
    MediaData mockMediaData = mock(MediaData.class);
    when(context.getContentId()).thenReturn(contentId);
    when(context.getData()).thenReturn(mockMediaData);

    byte[] payload = "test-data".getBytes();
    InputStream wrapped = new ClosingAwareInputStream(new ByteArrayInputStream(payload));
    when(mockOsClient.readContent(contentId)).thenReturn(CompletableFuture.completedFuture(wrapped));

    ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);

    // Act
    handler.readAttachment(context);

    // Assert: the stream handed to CAP is still readable (not prematurely closed)
    verify(mockMediaData).setContent(captor.capture());
    verify(context).setCompleted();

    InputStream handedOver = captor.getValue();
    byte[] read = handedOver.readAllBytes();
    assertArrayEquals(payload, read, "Stream was closed prematurely or content corrupted");
  }

  /**
   * InputStream wrapper that enforces close semantics for testing. Any read after close will throw
   * an IOException. This helps detect premature close in the handler.
   */
  private static class ClosingAwareInputStream extends InputStream {
    private final InputStream delegate;
    private boolean closed = false;

    ClosingAwareInputStream(InputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws java.io.IOException {
      if (closed) {
        throw new java.io.IOException("Stream is closed");
      }
      return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws java.io.IOException {
      if (closed) {
        throw new java.io.IOException("Stream is closed");
      }
      return delegate.read(b, off, len);
    }

    @Override
    public void close() throws java.io.IOException {
      closed = true;
      delegate.close();
    }
  }
}

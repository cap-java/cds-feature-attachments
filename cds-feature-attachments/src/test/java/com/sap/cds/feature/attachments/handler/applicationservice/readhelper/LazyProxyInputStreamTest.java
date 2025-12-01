/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.ServiceException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LazyProxyInputStreamTest {

  private LazyProxyInputStream cut;
  private InputStream inputStream;
  private AttachmentService attachmentService;
  private AttachmentStatusValidator attachmentStatusValidator;

  @BeforeEach
  void setup() {
    inputStream = mock(InputStream.class);
    attachmentService = mock(AttachmentService.class);
    attachmentStatusValidator = mock(AttachmentStatusValidator.class);
    when(attachmentService.readAttachment(any())).thenReturn(inputStream);
    cut =
        new LazyProxyInputStream(
            () -> attachmentService.readAttachment(any()),
            attachmentStatusValidator,
            StatusCode.CLEAN);
  }

  @Test
  void noMethodCallNoStreamAccess() {
    verifyNoInteractions(attachmentService);
  }

  @Test
  void simpleReadIsForwarded() throws IOException {
    when(inputStream.read()).thenReturn(12);

    var result = cut.read();

    verify(inputStream).read();
    assertThat(result).isEqualTo(12);
  }

  @Test
  @SuppressFBWarnings("RR_NOT_CHECKED")
  void readWithBytesIsForwarded() throws IOException {
    var bytes = "test".getBytes(StandardCharsets.UTF_8);
    when(inputStream.read(bytes)).thenReturn(24);

    var result = cut.read(bytes);

    verify(inputStream).read(bytes);
    assertThat(result).isEqualTo(24);
  }

  @Test
  @SuppressFBWarnings("RR_NOT_CHECKED")
  void readWithBytesAndParametersIsForwarded() throws IOException {
    var bytes = "test".getBytes(StandardCharsets.UTF_8);
    when(inputStream.read(bytes, 1, 2)).thenReturn(36);

    var result = cut.read(bytes, 1, 2);

    verify(inputStream).read(bytes, 1, 2);
    assertThat(result).isEqualTo(36);
  }

  @Test
  void supplierOnlyCalledOnce() throws IOException {
    when(inputStream.read()).thenReturn(48).thenReturn(60);

    var result1 = cut.read();
    var result2 = cut.read();

    verify(inputStream, times(2)).read();
    verify(attachmentService).readAttachment(any());
    assertThat(result1).isEqualTo(48);
    assertThat(result2).isEqualTo(60);
  }

  @Test
  void closeDoesNotCallSupplier() throws IOException {
    cut.close();

    verifyNoInteractions(inputStream);
    verifyNoInteractions(attachmentService);
  }

  @Test
  void closeCallsInputStream() throws IOException {
    cut.read();
    cut.close();

    verify(inputStream, times(1)).read();
    verify(inputStream, times(1)).close();
    verify(attachmentService).readAttachment(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {StatusCode.UNSCANNED, StatusCode.INFECTED})
  void exceptionIfWrongStatus(String status) {
    doThrow(AttachmentStatusException.class).when(attachmentStatusValidator).verifyStatus(status);

    cut =
        new LazyProxyInputStream(
            () -> attachmentService.readAttachment(any()), attachmentStatusValidator, status);

    assertThrows(AttachmentStatusException.class, () -> cut.read());
  }

  @Test
  void noExceptionIfCorrectStatus() {
    cut =
        new LazyProxyInputStream(
            () -> attachmentService.readAttachment(any()),
            attachmentStatusValidator,
            StatusCode.CLEAN);

    assertDoesNotThrow(() -> cut.read());
  }

  @Test
  void originalExceptionPreservedWhenSupplierFails() {
    // Test the core fix: original exception from supplier should be preserved
    // even when status validation also fails
    ServiceException originalException =
        new ServiceException("Failed to retrieve document content for file with ID: test123");
    AttachmentStatusException statusException = AttachmentStatusException.getNotCleanException();

    // Mock supplier to throw original exception
    when(attachmentService.readAttachment(any())).thenThrow(originalException);
    // Mock status validator to also throw exception
    doThrow(statusException).when(attachmentStatusValidator).verifyStatus(StatusCode.INFECTED);

    cut =
        new LazyProxyInputStream(
            () -> attachmentService.readAttachment(any()),
            attachmentStatusValidator,
            StatusCode.INFECTED);

    // The original exception should be thrown, not the status exception
    ServiceException thrownException = assertThrows(ServiceException.class, () -> cut.read());

    // Verify the original exception is preserved
    assertThat(thrownException).isEqualTo(originalException);
    assertThat(thrownException.getMessage())
        .isEqualTo("Failed to retrieve document content for file with ID: test123");

    // Verify the status exception is added as suppressed exception for context
    assertThat(thrownException.getSuppressed()).hasSize(1);
    assertThat(thrownException.getSuppressed()[0]).isInstanceOf(AttachmentStatusException.class);
  }

  @Test
  void originalExceptionPreservedWhenStatusValidationPasses() {
    // Test edge case: supplier fails but status validation passes
    // In this case, we still want the original exception, not status exception
    ServiceException originalException = new ServiceException("Network connection failed");

    // Mock supplier to throw original exception
    when(attachmentService.readAttachment(any())).thenThrow(originalException);
    // Status validator should pass (no exception thrown)

    cut =
        new LazyProxyInputStream(
            () -> attachmentService.readAttachment(any()),
            attachmentStatusValidator,
            StatusCode.CLEAN);

    // The original exception should be thrown
    ServiceException thrownException = assertThrows(ServiceException.class, () -> cut.read());

    // Verify the original exception is preserved
    assertThat(thrownException).isEqualTo(originalException);
    assertThat(thrownException.getMessage()).isEqualTo("Network connection failed");

    // No suppressed exceptions should be present since status validation passed
    assertThat(thrownException.getSuppressed()).isEmpty();
  }

  @Test
  void statusValidationOnlyWhenSupplierSucceeds() {
    // Test that status validation happens after successful stream creation
    // This ensures we don't pre-emptively block access due to status when the underlying issue
    // might be different

    cut =
        new LazyProxyInputStream(
            () -> attachmentService.readAttachment(any()),
            attachmentStatusValidator,
            StatusCode.CLEAN);

    assertDoesNotThrow(() -> cut.read());

    // Verify status was validated after supplier was called
    verify(attachmentService).readAttachment(any());
    verify(attachmentStatusValidator).verifyStatus(StatusCode.CLEAN);
  }
}

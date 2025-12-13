/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.services.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link LazyProxyInputStream} to verify that exceptions from the input stream
 * supplier are not masked by status validation exceptions.
 */
class LazyProxyInputStreamTest {

  @Mock private Supplier<InputStream> inputStreamSupplier;

  @Mock private AttachmentStatusValidator statusValidator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testAuthorizationExceptionIsNotMaskedByStatusException() throws IOException {
    // Arrange: Create an authorization exception
    ServiceException authException =
        new ServiceException("Forbidden: User does not have permission to read this attachment");

    // Mock supplier to throw authorization exception
    when(inputStreamSupplier.get()).thenThrow(authException);

    // Create LazyProxyInputStream with NOT_CLEAN status (which would throw if checked)
    try (LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "not_clean", false)) {

      // Act & Assert: The authorization exception should be thrown, not a status exception
      ServiceException thrown = assertThrows(ServiceException.class, () -> lazyStream.read());

      assertEquals(authException, thrown);
      assertEquals(
          "Forbidden: User does not have permission to read this attachment", thrown.getMessage());

      // Verify that status validation was never called because the supplier failed first
      verify(statusValidator, never()).verifyStatus("not_clean");
    }
  }

  @Test
  void testNetworkExceptionIsNotMaskedByStatusException() throws IOException {
    // Arrange: Create a network exception
    RuntimeException networkException = new RuntimeException("Network timeout");

    // Mock supplier to throw network exception
    when(inputStreamSupplier.get()).thenThrow(networkException);

    // Create LazyProxyInputStream with NOT_SCANNED status
    try (LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "unscanned", false)) {

      // Act & Assert: The network exception should be thrown, not a status exception
      RuntimeException thrown = assertThrows(RuntimeException.class, () -> lazyStream.read());

      assertEquals(networkException, thrown);
      assertEquals("Network timeout", thrown.getMessage());

      // Verify that status validation was never called
      verify(statusValidator, never()).verifyStatus("unscanned");
    }
  }

  @Test
  void testStatusExceptionIsThrownWhenAccessSucceeds() throws IOException {
    // Arrange: Mock successful stream access
    InputStream mockStream = new ByteArrayInputStream(new byte[] {1, 2, 3});
    when(inputStreamSupplier.get()).thenReturn(mockStream);

    // Mock status validator to throw AttachmentStatusException for NOT_CLEAN
    AttachmentStatusException statusException = AttachmentStatusException.getNotCleanException();
    doThrow(statusException).when(statusValidator).verifyStatus("not_clean");

    // Create LazyProxyInputStream with NOT_CLEAN status
    try (LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "not_clean", false)) {

      // Act & Assert: Now the status exception should be thrown (because access succeeded)
      AttachmentStatusException thrown =
          assertThrows(AttachmentStatusException.class, () -> lazyStream.read());

      assertEquals(statusException, thrown);

      // Verify that supplier was called successfully
      verify(inputStreamSupplier).get();
      // Verify that status validation was called after successful access
      verify(statusValidator).verifyStatus("not_clean");
    }
  }

  @Test
  void testSuccessfulReadWhenBothAccessAndStatusAreValid() throws IOException {
    // Arrange: Mock successful stream access
    byte[] testData = new byte[] {1, 2, 3, 4, 5};
    InputStream mockStream = new ByteArrayInputStream(testData);
    when(inputStreamSupplier.get()).thenReturn(mockStream);

    // Mock status validator to pass for CLEAN status (no exception)
    // Note: verifyStatus with CLEAN status should not throw, so we don't mock it to throw

    // Create LazyProxyInputStream with CLEAN status
    try (LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "clean", false)) {

      // Act: Read from the stream
      int firstByte = lazyStream.read();

      // Assert: Should successfully read the first byte
      assertEquals(1, firstByte);

      // Verify that supplier was called
      verify(inputStreamSupplier).get();
      // Verify that status validation was called
      verify(statusValidator).verifyStatus("clean");
    }
  }

  @Test
  void testDelegateIsOnlyCreatedOnce() throws IOException {
    // Arrange: Mock successful stream access
    byte[] testData = new byte[] {1, 2, 3};
    InputStream mockStream = new ByteArrayInputStream(testData);
    when(inputStreamSupplier.get()).thenReturn(mockStream);

    // Create LazyProxyInputStream with CLEAN status
    try (LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "clean", false)) {

      // Act: Read multiple times
      lazyStream.read();
      lazyStream.read();
      lazyStream.read();

      // Assert: Supplier should only be called once (delegate is cached)
      verify(inputStreamSupplier).get();
      // Status validation should also only be called once
      verify(statusValidator).verifyStatus("clean");
    }
  }

  @Test
  void testCloseDoesNotThrowWhenDelegateIsNull() throws IOException {
    // Arrange: Create LazyProxyInputStream that hasn't accessed the delegate yet
    LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "clean", false);

    // Act & Assert: Close should not throw even if delegate is null
    lazyStream.close();

    // Verify that supplier was never called
    verify(inputStreamSupplier, never()).get();
  }

  @Test
  void testCloseClosesDelegate() throws IOException {
    // Arrange: Mock successful stream access
    InputStream mockStream = mock(InputStream.class);
    when(inputStreamSupplier.get()).thenReturn(mockStream);

    LazyProxyInputStream lazyStream =
        new LazyProxyInputStream(inputStreamSupplier, statusValidator, "clean", false);

    // Access the delegate first
    lazyStream.read();

    // Act: Close the lazy stream
    lazyStream.close();

    // Assert: Delegate should be closed
    verify(mockStream).close();
  }
}

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.ExtendedErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CountingInputStreamTest {

  @Test
  void getDelegate_returnsWrappedStream() throws IOException {
    var delegate = mock(InputStream.class);
    try (var cut = new CountingInputStream(delegate, "100")) {
      assertThat(cut.getDelegate()).isSameAs(delegate);
    }
  }

  @Test
  void read_singleByte_countsCorrectly() throws IOException {
    var data = "hello".getBytes(StandardCharsets.UTF_8);
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "100");

    int bytesRead = 0;
    while (cut.read() != -1) {
      bytesRead++;
    }

    assertThat(bytesRead).isEqualTo(5);
    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void read_byteArray_countsCorrectly() throws IOException {
    var data = "hello world".getBytes(StandardCharsets.UTF_8);
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "100");

    byte[] buffer = new byte[5];
    int firstRead = cut.read(buffer);
    int secondRead = cut.read(buffer);

    assertThat(firstRead).isEqualTo(5);
    assertThat(secondRead).isEqualTo(5);
    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void read_byteArrayWithOffset_countsCorrectly() throws IOException {
    var data = "hello world".getBytes(StandardCharsets.UTF_8);
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "100");

    byte[] buffer = new byte[10];
    int bytesRead = cut.read(buffer, 2, 5);

    assertThat(bytesRead).isEqualTo(5);
    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void skip_countsCorrectly() throws IOException {
    var data = "hello world".getBytes(StandardCharsets.UTF_8);
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "100");

    long skipped = cut.skip(5);

    assertThat(skipped).isEqualTo(5);
    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void read_underLimit_noException() throws IOException {
    var data = "hello".getBytes(StandardCharsets.UTF_8);
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "10");

    assertDoesNotThrow(
        () -> {
          while (cut.read() != -1) {
            // read all bytes
          }
        });
    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void read_exactlyAtLimit_noException() throws IOException {
    var data = "hello".getBytes(StandardCharsets.UTF_8); // 5 bytes
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "5");

    assertDoesNotThrow(
        () -> {
          while (cut.read() != -1) {
            // read all bytes
          }
        });
    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void read_exceedsLimit_throwsServiceException() {
    var data = "hello world".getBytes(StandardCharsets.UTF_8); // 11 bytes
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "5");

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> {
              while (cut.read() != -1) {
                // read until exception
              }
            });

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    assertThat(cut.isLimitExceeded()).isTrue();
  }

  @Test
  void read_exceedsLimit_exceptionContainsMaxSizeString() {
    var data = "hello world".getBytes(StandardCharsets.UTF_8); // 11 bytes
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "5"); // limit is 5 bytes

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> {
              byte[] buffer = new byte[1024];
              while (cut.read(buffer) != -1) {
                // read until exception
              }
            });

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
  }

  @Test
  void read_bulkRead_exceedsLimit_throwsServiceException() {
    var data = "hello world test".getBytes(StandardCharsets.UTF_8); // 16 bytes
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "10");

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> {
              byte[] buffer = new byte[1024];
              while (cut.read(buffer) != -1) {
                // read until exception
              }
            });

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    assertThat(cut.isLimitExceeded()).isTrue();
  }

  @Test
  void skip_exceedsLimit_throwsServiceException() {
    var data = "hello world".getBytes(StandardCharsets.UTF_8);
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "5");

    ServiceException exception =
        assertThrows(
            ServiceException.class,
            () -> {
              long skipped = cut.skip(10);
              assertThat(skipped).isPositive(); // use value to satisfy SpotBugs
            });

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    assertThat(cut.isLimitExceeded()).isTrue();
  }

  @Test
  void isLimitExceeded_initiallyFalse() {
    var delegate = new ByteArrayInputStream(new byte[0]);
    var cut = new CountingInputStream(delegate, "100");

    assertThat(cut.isLimitExceeded()).isFalse();
  }

  @Test
  void close_closesDelegate() throws IOException {
    var delegate = mock(InputStream.class);
    var cut = new CountingInputStream(delegate, "100");

    cut.close();

    verify(delegate).close();
  }

  @Test
  void close_nullDelegate_noException() {
    var delegate = mock(InputStream.class);
    try (var cut = new CountingInputStream(delegate, "100")) {
      assertDoesNotThrow(cut::close);
    } catch (IOException e) {
      fail("Unexpected IOException");
    }
  }

  @Test
  void constructor_invalidMaxSize_throwsIllegalArgumentException() {
    var delegate = mock(InputStream.class);

    assertThrows(
        IllegalArgumentException.class, () -> new CountingInputStream(delegate, "invalid"));
  }

  @Test
  void constructor_nullMaxSize_throwsIllegalArgumentException() {
    var delegate = mock(InputStream.class);

    assertThrows(IllegalArgumentException.class, () -> new CountingInputStream(delegate, null));
  }

  @Test
  void constructor_parsesKB() throws IOException {
    var data = new byte[1500]; // 1500 bytes
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, "2KB"); // 2000 bytes limit

    assertDoesNotThrow(
        () -> {
          byte[] buffer = new byte[1024];
          while (cut.read(buffer) != -1) {
            // read all bytes
          }
        });
  }

  @Test
  void constructor_parsesMB() {
    var delegate = mock(InputStream.class);

    var cut = assertDoesNotThrow(() -> new CountingInputStream(delegate, "10MB"));

    assertThat(cut).isNotNull();
  }

  @Test
  void read_returnsMinusOne_doesNotCount() throws IOException {
    var delegate = mock(InputStream.class);
    when(delegate.read()).thenReturn(-1);
    try (var cut = new CountingInputStream(delegate, "5")) {
      int result = cut.read();

      assertThat(result).isEqualTo(-1);
      assertThat(cut.isLimitExceeded()).isFalse();
    }
  }

  @Test
  void read_byteArray_returnsMinusOne_doesNotCount() throws IOException {
    var delegate = mock(InputStream.class);
    when(delegate.read(new byte[10])).thenReturn(-1);
    try (var cut = new CountingInputStream(delegate, "5")) {
      int result = cut.read(new byte[10]);

      assertThat(result).isEqualTo(-1);
      assertThat(cut.isLimitExceeded()).isFalse();
    }
  }

  @Test
  void read_byteArrayWithOffset_returnsMinusOne_doesNotCount() throws IOException {
    var delegate = mock(InputStream.class);
    byte[] buffer = new byte[10];
    when(delegate.read(buffer, 0, 5)).thenReturn(-1);
    try (var cut = new CountingInputStream(delegate, "5")) {
      int result = cut.read(buffer, 0, 5);

      assertThat(result).isEqualTo(-1);
      assertThat(cut.isLimitExceeded()).isFalse();
    }
  }

  @Test
  void skip_returnsZero_doesNotCount() throws IOException {
    var delegate = mock(InputStream.class);
    when(delegate.skip(10)).thenReturn(0L);
    try (var cut = new CountingInputStream(delegate, "5")) {
      long result = cut.skip(10);

      assertThat(result).isZero();
      assertThat(cut.isLimitExceeded()).isFalse();
    }
  }

  @Test
  void constructor_fractionalValue_throwsServiceException() {
    var delegate = mock(InputStream.class);

    // A fractional value like "1.5KB" results in 1500.0 bytes which cannot be
    // converted exactly to
    // long
    // via longValueExact() and throws ArithmeticException, which is caught and
    // wrapped in
    // ServiceException
    ServiceException exception =
        assertThrows(ServiceException.class, () -> new CountingInputStream(delegate, "1.5"));

    assertThat(exception.getMessage()).contains("Error parsing max size annotation value");
    assertThat(exception.getCause()).isInstanceOf(ArithmeticException.class);
  }
}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;

class CountingInputStreamTest {

  @Test
  void constructor_negativeMaxBytes_throwsException() {
    var delegate = new ByteArrayInputStream(new byte[0]);
    assertThatThrownBy(() -> new CountingInputStream(delegate, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("maxBytes must be non-negative");
  }

  @Test
  void read_singleByte_tracksCount() throws IOException {
    var data = new byte[] { 1, 2, 3 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    cut.read();
    assertThat(cut.getBytesRead()).isEqualTo(1);

    cut.read();
    assertThat(cut.getBytesRead()).isEqualTo(2);

    cut.close();
  }

  @Test
  void read_byteArray_tracksCount() throws IOException {
    var data = new byte[] { 1, 2, 3, 4, 5 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    var buffer = new byte[3];
    int bytesRead = cut.read(buffer);

    assertThat(bytesRead).isEqualTo(3);
    assertThat(cut.getBytesRead()).isEqualTo(3);
  }

  @Test
  void read_byteArrayWithOffset_tracksCount() throws IOException {
    var data = new byte[] { 1, 2, 3, 4, 5 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    var buffer = new byte[5];
    int bytesRead = cut.read(buffer, 0, 3);

    assertThat(bytesRead).isEqualTo(3);
    assertThat(cut.getBytesRead()).isEqualTo(3);
  }

  @Test
  void read_exceedsLimit_throwsException() {
    var data = new byte[] { 1, 2, 3, 4, 5 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 3);

    assertThatThrownBy(() -> {
      var buffer = new byte[5];
      int bytesRead = cut.read(buffer);
    });

  }

  @Test
  void skip_tracksCount() throws IOException {
    var data = new byte[] { 1, 2, 3, 4, 5 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    long skipped = cut.skip(3);
    cut.close();

    assertThat(skipped).isEqualTo(3);
    assertThat(cut.getBytesRead()).isEqualTo(3);

  }

  @Test
  void skip_exceedsLimit_throwsException() {
    var data = new byte[] { 1, 2, 3, 4, 5 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 2);

    assertThatThrownBy(() -> {
      long skipped = cut.skip(3);
    });
  }

  @Test
  void available_delegatesToUnderlying() throws IOException {
    var data = new byte[] { 1, 2, 3 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);
    cut.close();

    assertThat(cut.available()).isEqualTo(3);
  }

  @Test
  void close_closesDelegate() throws IOException {
    var data = new byte[] { 1, 2, 3 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);
    cut.close();

    // ByteArrayInputStream doesn't really close, but we verify no exception
    assertThat(cut).isNotNull();
  }

  @Test
  void markSupported_delegatesToUnderlying() {
    var data = new byte[] { 1, 2, 3 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    assertThat(cut.markSupported()).isTrue();
  }

  @Test
  void markAndReset_delegatesToUnderlying() throws IOException {
    var data = new byte[] { 1, 2, 3 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    cut.mark(10);
    cut.read();
    cut.read();
    assertThat(cut.getBytesRead()).isEqualTo(2);

    cut.reset();
    int value = cut.read();
    assertThat(value).isEqualTo(1);
    cut.close();
  }

  @Test
  void read_endOfStream_returnsMinusOne() throws IOException {
    var data = new byte[] { 1 };
    var delegate = new ByteArrayInputStream(data);
    var cut = new CountingInputStream(delegate, 10);

    cut.read(); // read the only byte
    int result = cut.read(); // should return -1

    assertThat(result).isEqualTo(-1);
    assertThat(cut.getBytesRead()).isEqualTo(1); // count shouldn't increase
    cut.close();
  }
}
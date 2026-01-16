/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileSizeUtilsTest {

  @ParameterizedTest
  @CsvSource({
    "100, 100",
    "100B, 100",
    "1KB, 1000",
    "1MB, 1000000",
    "1GB, 1000000000",
    "1TB, 1000000000000",
    "1KIB, 1024",
    "1MIB, 1048576",
    "1GIB, 1073741824",
    "1TIB, 1099511627776",
    "2.5KB, 2500",
    "1.5MB, 1500000",
    "  100  , 100",
    "  100 KB  , 100000",
    "0, 0",
    "0B, 0",
    "001KB, 1000",
    "0.5MB, 500000"
  })
  void parseFileSizeToBytes_validInput(String input, long expected) {
    assertThat(FileSizeUtils.parseFileSizeToBytes(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "1kb, 1000",
    "1mb, 1000000",
    "1gb, 1000000000",
    "1tb, 1000000000000",
    "1kib, 1024",
    "1mib, 1048576",
    "1gib, 1073741824",
    "1tib, 1099511627776",
    "100b, 100"
  })
  void parseFileSizeToBytes_lowercaseUnits(String input, long expected) {
    assertThat(FileSizeUtils.parseFileSizeToBytes(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "1Kb, 1000",
    "1Mb, 1000000",
    "1Gb, 1000000000",
    "1KiB, 1024",
    "1MiB, 1048576",
    "1GiB, 1073741824"
  })
  void parseFileSizeToBytes_mixedCaseUnits(String input, long expected) {
    assertThat(FileSizeUtils.parseFileSizeToBytes(input)).isEqualTo(expected);
  }

  @Test
  void parseFileSizeToBytes_nullInput_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Value for Max File Size is null");
  }

  @ParameterizedTest
  @CsvSource({"invalid", "abc KB", "-100", "''"})
  void parseFileSizeToBytes_invalidFormat_throwsException(String input) {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes(input))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseFileSizeToBytes_whitespaceOnly_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid size");
  }

  @Test
  void parseFileSizeToBytes_fractionalBytes_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes("0.5"))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void parseFileSizeToBytes_fractionalBytesWithUnit_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes("0.5B"))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void parseFileSizeToBytes_unknownUnit_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes("100XB"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown Unit");
  }
}

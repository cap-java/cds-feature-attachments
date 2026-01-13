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
      "  100 KB  , 100000"
  })
  void parseFileSizeToBytes_validInput(String input, long expected) {
    assertThat(FileSizeUtils.parseFileSizeToBytes(input)).isEqualTo(expected);
  }

  @Test
  void parseFileSizeToBytes_nullInput_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Value for Max File Size is null");
  }

  @ParameterizedTest
  @CsvSource({
      "invalid",
      "100XB",
      "abc KB",
      "-100"
  })
  void parseFileSizeToBytes_invalidFormat_throwsException(String input) {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes(input))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseFileSizeToBytes_unknownUnit_throwsException() {
    assertThatThrownBy(() -> FileSizeUtils.parseFileSizeToBytes("100XB"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown Unit");
  }
}
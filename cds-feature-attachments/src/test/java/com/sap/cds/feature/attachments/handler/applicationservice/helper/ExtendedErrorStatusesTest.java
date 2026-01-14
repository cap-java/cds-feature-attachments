/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExtendedErrorStatusesTest {

  @Test
  void contentTooLargeHasCorrectProperties() {
    assertThat(ExtendedErrorStatuses.CONTENT_TOO_LARGE.getCodeString()).isEqualTo("413");
    assertThat(ExtendedErrorStatuses.CONTENT_TOO_LARGE.getDescription())
        .isEqualTo("The content size exceeds the maximum allowed limit.");
    assertThat(ExtendedErrorStatuses.CONTENT_TOO_LARGE.getHttpStatus()).isEqualTo(413);
  }

  @Test
  void getByCode_existingCode_returnsErrorStatus() {
    var result = ExtendedErrorStatuses.getByCode(400);
    assertThat(result).isNotNull();
  }

  @Test
  void getByCode_nonExistingCode_returnsNull() {
    var result = ExtendedErrorStatuses.getByCode(999);
    assertThat(result).isNull();
  }
}

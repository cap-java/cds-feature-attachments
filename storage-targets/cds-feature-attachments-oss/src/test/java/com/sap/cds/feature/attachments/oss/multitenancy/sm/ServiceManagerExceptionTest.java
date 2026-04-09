/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceManagerExceptionTest {

  @Test
  void testExceptionWithMessage() {
    var ex = new ServiceManagerException("something failed");

    assertThat(ex.getMessage()).isEqualTo("something failed");
    assertThat(ex.getCause()).isNull();
  }

  @Test
  void testExceptionWithMessageAndCause() {
    var cause = new RuntimeException("root");
    var ex = new ServiceManagerException("something failed", cause);

    assertThat(ex.getMessage()).isEqualTo("something failed");
    assertThat(ex.getCause()).isSameAs(cause);
  }
}

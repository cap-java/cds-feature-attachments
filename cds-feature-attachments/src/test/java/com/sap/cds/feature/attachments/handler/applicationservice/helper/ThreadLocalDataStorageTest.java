/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ThreadLocalDataStorageTest {

  private ThreadLocalDataStorage cut;

  @BeforeEach
  void setup() {
    cut = new ThreadLocalDataStorage();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void valueCanBetSetAndRetrieved(Boolean value) {
    var valueToSet = new AtomicBoolean();

    cut.set(value, () -> valueToSet.set(cut.get()));

    assertThat(valueToSet.get()).isEqualTo(value);
  }

  @Test
  void valueIsRemoved() {
    cut.set(true, () -> {});

    assertThat(cut.get()).isFalse();
  }
}

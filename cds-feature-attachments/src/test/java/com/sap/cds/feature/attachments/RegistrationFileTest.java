/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.feature.attachments.configuration.Registration;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationFileTest {

  @Test
  void registrationFileAvailable() throws IOException {
    String path =
        "src/main/resources/META-INF/services/com.sap.cds.services.runtime.CdsRuntimeConfiguration";

    BufferedReader reader = Files.newBufferedReader(Paths.get(path));
    List<String> classes = reader.lines().toList();
    reader.close();

    String runtimeRegistrationName = Registration.class.getCanonicalName();

    assertThat(classes).contains(runtimeRegistrationName);
  }
}

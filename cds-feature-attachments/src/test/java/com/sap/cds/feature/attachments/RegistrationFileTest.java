package com.sap.cds.feature.attachments;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.configuration.Registration;

class RegistrationFileTest {

	@Test
	void registrationFileAvailable() throws IOException {
		String path =
		"src/main/resources/META-INF/services/com.sap.cds.services.runtime.CdsRuntimeConfiguration";

		BufferedReader reader = Files.newBufferedReader(Paths.get(path));
		List<String> classes = reader.lines().collect(Collectors.toList());
		reader.close();

		String runtimeRegistrationName = Registration.class.getCanonicalName();

		assertThat(classes).contains(runtimeRegistrationName);
	}

}

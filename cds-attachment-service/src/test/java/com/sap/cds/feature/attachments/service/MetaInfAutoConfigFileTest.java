package com.sap.cds.feature.attachments.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.configuration.AutoConfiguration;

class MetaInfAutoConfigFileTest {

		@Test
		void testAutoconfigFactories() throws IOException {
				String path =
						"src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

				BufferedReader reader = Files.newBufferedReader(Paths.get(path));
				List<String> classes = reader.lines().collect(Collectors.toList());
				reader.close();

				String configurationClass = AutoConfiguration.class.getCanonicalName();

				assertThat(classes).contains(configurationClass);
		}

}

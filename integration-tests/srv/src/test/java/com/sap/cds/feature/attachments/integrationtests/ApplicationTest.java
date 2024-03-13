package com.sap.cds.feature.attachments.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ApplicationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void checkApplicationContextCanBeLoaded() {
		assertThat(context).isNotNull();
	}

	@Test
	void noExceptionIsThrown() {
		String[] args = new String[0];
		assertDoesNotThrow(() -> Application.main(args));
	}

}

/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void checkApplicationContextCanBeLoaded() {
		assertThat(context).isNotNull();
	}

}

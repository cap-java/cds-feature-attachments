/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeletionUserInfoTest {

	private DeletionUserInfo cut;

	@BeforeEach
	void setup() {
		cut = DeletionUserInfo.create();
	}

	@Test
	void dataCanBeRead() {
		var name = "some name";

		cut.setName(name);

		assertThat(cut.getName()).isEqualTo(name);
	}

	@Test
	void dataCanBeReadWithConstant() {
		var name = "some_name";

		cut.setName(name);

		assertThat(cut).containsEntry(DeletionUserInfo.NAME, name);
	}

}

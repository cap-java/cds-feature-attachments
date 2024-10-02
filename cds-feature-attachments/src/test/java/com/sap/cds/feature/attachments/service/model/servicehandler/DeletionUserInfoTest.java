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
		var id = "some id";
		var name = "some name";
		var tenant = "some tenant";

		cut.setId(id);
		cut.setName(name);
		cut.setTenant(tenant);

		assertThat(cut.getId()).isEqualTo(id);
		assertThat(cut.getName()).isEqualTo(name);
		assertThat(cut.getTenant()).isEqualTo(tenant);
	}

	@Test
	void dataCanBeReadWithConstant() {
		var id = "some_id";
		var name = "some_name";
		var tenant = "some_tenant";

		cut.setId(id);
		cut.setName(name);
		cut.setTenant(tenant);

		assertThat(cut).containsEntry(DeletionUserInfo.ID, id);
		assertThat(cut).containsEntry(DeletionUserInfo.NAME, name);
		assertThat(cut).containsEntry(DeletionUserInfo.TENANT, tenant);
	}

}
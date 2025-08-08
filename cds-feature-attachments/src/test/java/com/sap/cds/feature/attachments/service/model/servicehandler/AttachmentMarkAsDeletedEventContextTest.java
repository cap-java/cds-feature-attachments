/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachmentMarkAsDeletedEventContextTest {

	private AttachmentMarkAsDeletedEventContext cut;

	@BeforeEach
	void setup() {
		cut = AttachmentMarkAsDeletedEventContext.create();
	}

	@Test
	void fieldsCanBeSetAndRead() {
		cut.setContentId("document id");

		assertThat(cut.getContentId()).isEqualTo("document id");
	}

}

/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.service.model.servicehandler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachmentRestoreEventContextTest {

	private AttachmentRestoreEventContext cut;

	@BeforeEach
	void setup() {
		cut = AttachmentRestoreEventContext.create();
	}

	@Test
	void fieldsCanBeSetAndRead() {
		var timestamp = Instant.now();
		cut.setRestoreTimestamp(timestamp);

		assertThat(cut.getRestoreTimestamp()).isEqualTo(timestamp);
	}

}

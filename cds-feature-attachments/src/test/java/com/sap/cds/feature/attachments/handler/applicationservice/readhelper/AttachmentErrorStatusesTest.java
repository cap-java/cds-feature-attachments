/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachmentErrorStatusesTest {

	@Test
	void notCleanCodeHasCorrectProperties() {
		assertThat(AttachmentErrorStatuses.NOT_CLEAN.getCodeString()).isEqualTo("not_clean");
		assertThat(AttachmentErrorStatuses.NOT_CLEAN.getDescription()).isEqualTo("Attachment is not clean");
		assertThat(AttachmentErrorStatuses.NOT_CLEAN.getHttpStatus()).isEqualTo(405);
	}

	@Test
	void notScannedCodeHasCorrectProperties() {
		assertThat(AttachmentErrorStatuses.NOT_SCANNED.getCodeString()).isEqualTo("not_scanned");
		assertThat(AttachmentErrorStatuses.NOT_SCANNED.getDescription()).isEqualTo(
				"Attachment is not scanned, try again in a few minutes");
		assertThat(AttachmentErrorStatuses.NOT_SCANNED.getHttpStatus()).isEqualTo(405);
	}

}

package com.sap.cds.feature.attachments.utilities;

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
		assertThat(AttachmentErrorStatuses.NOT_SCANNED.getDescription()).isEqualTo("Attachment is not scanned");
		assertThat(AttachmentErrorStatuses.NOT_SCANNED.getHttpStatus()).isEqualTo(405);
	}

}

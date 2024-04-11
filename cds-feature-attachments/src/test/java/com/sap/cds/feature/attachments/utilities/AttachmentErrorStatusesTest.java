package com.sap.cds.feature.attachments.utilities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AttachmentErrorStatusesTest {

	@Test
	void notCleanCodeHasCorrectProperties() {
		assertThat(AttachmentErrorStatuses.NOT_CLEAN.getCodeString()).isEqualTo("no_clean");
		assertThat(AttachmentErrorStatuses.NOT_CLEAN.getDescription()).isEqualTo("Attachment is not clean");
		assertThat(AttachmentErrorStatuses.NOT_CLEAN.getHttpStatus()).isEqualTo(405);
	}

}

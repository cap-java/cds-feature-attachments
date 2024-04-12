package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;

class AttachmentStatusExceptionTest {

	@Test
	void correctValuesUsedForNotClean() {
		AttachmentStatusException exception = AttachmentStatusException.getNotCleanException();
		assertThat(exception.getLocalizedMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getDescription());
		assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getCodeString());
		assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN);
	}

	@Test
	void correctValuesUsedForNotScanned() {
		AttachmentStatusException exception = AttachmentStatusException.getNotScannedException();
		assertThat(exception.getLocalizedMessage()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED.getDescription());
		assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED.getCodeString());
		assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED);
	}

}

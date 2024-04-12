package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;

class AttachmentStatusExceptionTest {

	@Test
	void correctValuesUsed() {
		AttachmentStatusException exception = new AttachmentStatusException();
		assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getCodeString());
		assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN);
	}

}

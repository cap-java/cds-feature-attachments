/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.helper.LogObserver;

class AttachmentStatusValidatorTest {

	private AttachmentStatusValidator cut;
	private LogObserver observer;

	@BeforeEach
	void setup() {
		cut = new AttachmentStatusValidator();
		observer = LogObserver.create(cut.getClass().getName());
	}

	@AfterEach
	void tearDown() {
		observer.stop();
	}

	@Test
	void noExceptionIsThrown() {
		assertDoesNotThrow(() -> cut.verifyStatus(StatusCode.CLEAN));
	}

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.INFECTED, StatusCode.UNSCANNED, StatusCode.SCANNING, "some other status"})
	void exceptionIsThrown(String status) {
		var exception = assertThrows(AttachmentStatusException.class, () -> cut.verifyStatus(status));
		if (StatusCode.UNSCANNED.equals(status) || StatusCode.SCANNING.equals(status)) {
			assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED);
			assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED.getCodeString());
			assertThat(exception.getLocalizedMessage()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED.getDescription());
		} else {
			assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN);
			assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getCodeString());
			assertThat(exception.getLocalizedMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getDescription());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.CLEAN, StatusCode.UNSCANNED, StatusCode.INFECTED, "some other status"})
	void noLogsWritten(String status) {
		observer.start();

		try {
			cut.verifyStatus(status);
		} catch (AttachmentStatusException ignored) {
			// ignore
		}

		observer.stop();
		assertThat(observer.getLogEvents()).isEmpty();
	}

}

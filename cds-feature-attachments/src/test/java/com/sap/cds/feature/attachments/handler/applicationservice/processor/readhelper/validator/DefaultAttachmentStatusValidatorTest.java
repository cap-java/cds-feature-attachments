package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;
import com.sap.cds.feature.attachments.helper.LogObserver;
import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;

import ch.qos.logback.classic.Level;

class DefaultAttachmentStatusValidatorTest {

	private DefaultAttachmentStatusValidator cut;
	private LogObserver observer;

	@BeforeEach
	void setup() {
		cut = new DefaultAttachmentStatusValidator();
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
	@ValueSource(strings = {StatusCode.INFECTED, StatusCode.UNSCANNED, "some other status"})
	void exceptionIsThrown(String status) {
		var exception = assertThrows(AttachmentStatusException.class, () -> cut.verifyStatus(status));
		if (StatusCode.UNSCANNED.equals(status)) {
			assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED);
			assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED.getCodeString());
			assertThat(exception.getLocalizedMessage()).isEqualTo(AttachmentErrorStatuses.NOT_SCANNED.getDescription());
		} else {
			assertThat(exception.getErrorStatus()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN);
			assertThat(exception.getPlainMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getCodeString());
			assertThat(exception.getLocalizedMessage()).isEqualTo(AttachmentErrorStatuses.NOT_CLEAN.getDescription());
		}
	}

	@Test
	void noScannerStatusLogsWarning() {
		observer.start();

		cut.verifyStatus(StatusCode.CLEAN);

		observer.stop();
		var list = observer.getLogEvents();
		assertThat(list).hasSize(1);
		list.forEach(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.WARN);
			assertThat(event.getFormattedMessage()).isNotEmpty();
		});
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

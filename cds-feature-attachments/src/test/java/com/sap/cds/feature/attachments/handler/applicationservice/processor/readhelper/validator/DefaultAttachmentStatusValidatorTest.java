package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;
import com.sap.cds.feature.attachments.helper.LogObserver;

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

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.CLEAN, StatusCode.NO_SCANNER})
	void noExceptionIsThrown(String status) {
		assertDoesNotThrow(() -> cut.verifyStatus(status));
	}

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.INFECTED, StatusCode.UNSCANNED, "some other status"})
	void exceptionIsThrown(String status) {
		assertThrows(AttachmentStatusException.class, () -> cut.verifyStatus(status));
	}

	@Test
	void noScannerStatusLogsWarning() {
		observer.start();

		cut.verifyStatus(StatusCode.NO_SCANNER);

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

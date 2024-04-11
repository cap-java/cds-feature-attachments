package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class DefaultAttachmentStatusValidatorTest {

	private DefaultAttachmentStatusValidator cut;

	@BeforeEach
	void setup() {
		cut = new DefaultAttachmentStatusValidator();
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
		var appender = addAndStartLogAppender();

		cut.verifyStatus(StatusCode.NO_SCANNER);

		appender.stop();
		var list = appender.list;
		assertThat(list).hasSize(1);
		appender.list.forEach(event -> {
			assertThat(event.getLevel()).isEqualTo(Level.WARN);
			assertThat(event.getFormattedMessage()).isNotEmpty();
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.CLEAN, StatusCode.UNSCANNED, StatusCode.INFECTED, "some other status"})
	void noLogsWritten(String status) {
		var appender = addAndStartLogAppender();

		try {
			cut.verifyStatus(status);
		} catch (AttachmentStatusException e) {
			// ignore
		}

		appender.stop();
		assertThat(appender.list).isEmpty();
	}

	private ListAppender<ILoggingEvent> addAndStartLogAppender() {
		var logger = (Logger) LoggerFactory.getLogger(cut.getClass().getName());
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		logger.addAppender(appender);
		appender.start();
		return appender;
	}

}

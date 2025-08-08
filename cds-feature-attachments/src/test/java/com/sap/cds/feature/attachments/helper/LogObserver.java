/*
 * © 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.helper;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class LogObserver {

	private final ListAppender<ILoggingEvent> appender;
	private final Logger logger;

	private LogObserver(Logger logger) {
		this.logger = logger;
		this.appender = new ListAppender<>();
	}

	public static LogObserver create(String className) {
		var logger = (Logger) LoggerFactory.getLogger(className);
		var observer = new LogObserver(logger);
		observer.logger.addAppender(observer.appender);
		return observer;
	}

	public void start() {
		appender.start();
	}

	public void stop() {
		appender.stop();
		logger.detachAppender(appender);
	}

	public void setLevel(Level level) {
		logger.setLevel(level);
	}

	public List<ILoggingEvent> getLogEvents() {
		return appender.list;
	}

}

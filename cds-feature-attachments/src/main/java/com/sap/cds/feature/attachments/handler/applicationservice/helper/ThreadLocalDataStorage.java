/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

public class ThreadLocalDataStorage implements ThreadDataStorageSetter, ThreadDataStorageReader {

	private static final ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();

	@Override
	public void set(boolean value, Runnable runnable) {
		try {
			threadLocal.set(value);
			runnable.run();
		} finally {
			threadLocal.remove();
		}
	}

	@Override
	public boolean get() {
		return Boolean.TRUE.equals(threadLocal.get());
	}

}

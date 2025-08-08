/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

public interface ThreadDataStorageSetter {

	void set(boolean value, Runnable runnable);

}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.transaction;

import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.runtime.CdsRuntime;

/**
 * The interface {@link ListenerProvider} provides a method to provide a listener
 * for an after transaction closed processing.
 */
public interface ListenerProvider {

	ChangeSetListener provideListener(String contentId, CdsRuntime cdsRuntime);

}

/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction;

import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.runtime.CdsRuntime;

/**
	* The interface {@link ListenerProvider} provides a method to provide a listener
	* for an after transaction closed processing.
	*/
public interface ListenerProvider {

	ChangeSetListener provideListener(String documentId, CdsRuntime cdsRuntime);

}

/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.utilities;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public enum LoggingMarker {

	ATTACHMENT_SERVICE_REGISTRATION,

	APPLICATION_CREATE_HANDLER,
	APPLICATION_UPDATE_HANDLER,
	APPLICATION_DELETE_HANDLER,
	APPLICATION_READ_HANDLER,

	DRAFT_PATCH_HANDLER,
	DRAFT_CANCEL_HANDLER,

	ATTACHMENT_SERVICE_CREATE_METHOD,
	ATTACHMENT_SERVICE_DELETE_METHOD,
	ATTACHMENT_SERVICE_RESTORE_METHOD,
	ATTACHMENT_SERVICE_READ_METHOD,

	ATTACHMENT_SERVICE_CREATE_HANDLER,
	ATTACHMENT_SERVICE_DELETE_HANDLER,
	ATTACHMENT_SERVICE_RESTORE_HANDLER,
	ATTACHMENT_SERVICE_READ_HANDLER,

	MALWARE_SCAN_SCAN_METHOD,

	MALWARE_SCAN_SERVICE_SCAN_HANDLER,
	MALWARE_SCANNER;

	public Marker getMarker() {
		return MarkerFactory.getMarker(name());
	}

}

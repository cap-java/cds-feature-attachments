/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.utilities;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public enum LoggingMarker {

	ATTACHMENT_SERVICE_REGISTRATION,

	APPLICATION_HANDLER,

	DRAFT_HANDLER,

	ATTACHMENT_SERVICE,

	MALWARE_SCAN_SCAN_METHOD,

	MALWARE_SCAN_SERVICE_SCAN_HANDLER,
	MALWARE_SCANNER;

	public Marker getMarker() {
		return MarkerFactory.getMarker(name());
	}

}

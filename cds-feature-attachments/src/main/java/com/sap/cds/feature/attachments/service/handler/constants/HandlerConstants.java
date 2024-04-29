/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.handler.constants;

import com.sap.cds.services.handler.annotations.HandlerOrder;

public final class HandlerConstants {

	public static final int DEFAULT_ON = 10 * HandlerOrder.AFTER + HandlerOrder.LATE;

	private HandlerConstants() {
		// Prevent instantiation
	}
}

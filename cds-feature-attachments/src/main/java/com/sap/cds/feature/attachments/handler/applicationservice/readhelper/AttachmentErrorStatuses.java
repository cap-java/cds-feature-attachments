/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.ErrorStatuses;

/**
 * Error statuses for the attachment service.
 */
enum AttachmentErrorStatuses implements ErrorStatus {

	/**
	 * The attachment is not clean.
	 */
	NOT_CLEAN("not_clean", "Attachment is not clean", ErrorStatuses.METHOD_NOT_ALLOWED),

	/**
	 * The attachment is not scanned.
	 */
	NOT_SCANNED("not_scanned", "Attachment is not scanned, try again in a few minutes",
			ErrorStatuses.METHOD_NOT_ALLOWED);

	private final String code;
	private final String description;
	private final ErrorStatus httpError;

	AttachmentErrorStatuses(String code, String description, ErrorStatus httpError) {
		this.code = code;
		this.description = description;
		this.httpError = httpError;
	}

	@Override
	public String getCodeString() {
		return code;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getHttpStatus() {
		return httpError.getHttpStatus();
	}

}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.utils.ErrorStatusException;

/**
 * Exception for attachment status errors.
 * This exception is thrown when an attachment is not in a clean or scanned state.
 */
public class AttachmentStatusException extends ErrorStatusException {

	private static final long serialVersionUID = 1L;

	private AttachmentStatusException(ErrorStatus errorStatus) {
		super(errorStatus);
	}

	/**
	 * Creates an {@link AttachmentStatusException} indicating that the attachment is not clean.
	 *
	 * @return AttachmentStatusException for not clean attachment
	 */
	public static AttachmentStatusException getNotCleanException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_CLEAN);
	}

	/**
	 * Creates an {@link AttachmentStatusException} indicating that the attachment is not scanned.
	 *
	 * @return AttachmentStatusException for not scanned attachment
	 */
	public static AttachmentStatusException getNotScannedException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_SCANNED);
	}

}

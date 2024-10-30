/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception;

import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;
import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.utils.ErrorStatusException;

public class AttachmentStatusException extends ErrorStatusException {

	private AttachmentStatusException(ErrorStatus errorStatus) {
		super(errorStatus);
	}

	public static AttachmentStatusException getNotCleanException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_CLEAN);
	}

	public static AttachmentStatusException getNotScannedException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_SCANNED);
	}

}

/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper;

import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;
import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.utils.ErrorStatusException;

public class AttachmentStatusException extends ErrorStatusException {

	private static final long serialVersionUID = 743951900719373778L;

	private AttachmentStatusException(ErrorStatus errorStatus, String message) {
		super(errorStatus, message);
	}

	static AttachmentStatusException getNotCleanException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_CLEAN,
				AttachmentErrorStatuses.NOT_CLEAN.getDescription());
	}

	static AttachmentStatusException getNotScannedException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_SCANNED,
				AttachmentErrorStatuses.NOT_SCANNED.getDescription());
	}

}

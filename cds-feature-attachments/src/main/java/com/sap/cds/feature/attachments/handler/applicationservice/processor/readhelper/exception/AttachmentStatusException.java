package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception;

import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;
import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.utils.ErrorStatusException;

public class AttachmentStatusException extends ErrorStatusException {

	private AttachmentStatusException(ErrorStatus errorStatus, String message) {
		super(errorStatus, message);
	}

	public static AttachmentStatusException getNotCleanException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_CLEAN,
				AttachmentErrorStatuses.NOT_CLEAN.getDescription());
	}

	public static AttachmentStatusException getNotScannedException() {
		return new AttachmentStatusException(AttachmentErrorStatuses.NOT_SCANNED,
				AttachmentErrorStatuses.NOT_SCANNED.getDescription());
	}

}

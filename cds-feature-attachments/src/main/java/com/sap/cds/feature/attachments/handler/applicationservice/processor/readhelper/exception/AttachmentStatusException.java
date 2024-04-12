package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception;

import com.sap.cds.feature.attachments.utilities.AttachmentErrorStatuses;
import com.sap.cds.services.ServiceException;

public class AttachmentStatusException extends ServiceException {

	public AttachmentStatusException() {
		super(AttachmentErrorStatuses.NOT_CLEAN, AttachmentErrorStatuses.NOT_CLEAN.getCodeString());
	}

}

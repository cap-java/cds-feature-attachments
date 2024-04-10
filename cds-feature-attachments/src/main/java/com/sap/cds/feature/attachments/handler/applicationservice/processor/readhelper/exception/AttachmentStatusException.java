package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;

public class AttachmentStatusException extends ServiceException {

	public AttachmentStatusException() {
		//TODO enable translation for consumers
		super(ErrorStatuses.METHOD_NOT_ALLOWED, "Attachment is not clean");
	}

}

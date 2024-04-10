package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;

public class DefaultAttachmentStatusValidator implements AttachmentStatusValidator {

	//TODO unit tests
	@Override
	public void verifyStatus(String attachmentStatus) {
		if (!StatusCode.CLEAN.equals(attachmentStatus) && !StatusCode.NO_SCANNER.equals(attachmentStatus)) {
			//TODO warning log if no_scanner
			throw new AttachmentStatusException();
		}
	}

}

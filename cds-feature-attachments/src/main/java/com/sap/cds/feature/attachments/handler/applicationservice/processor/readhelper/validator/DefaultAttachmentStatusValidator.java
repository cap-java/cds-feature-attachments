/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;

public class DefaultAttachmentStatusValidator implements AttachmentStatusValidator {

	@Override
	public void verifyStatus(String attachmentStatus) {
		if (!StatusCode.CLEAN.equals(attachmentStatus)) {
			throw StatusCode.UNSCANNED.equals(attachmentStatus) || StatusCode.SCANNING.equals(
					attachmentStatus) ? AttachmentStatusException.getNotScannedException() : AttachmentStatusException.getNotCleanException();
		}
	}

}

/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;

public class AttachmentStatusValidator {

	/**
	 * This method validates the status of the attachment. If the status is not clean, then it throws an exception.
	 * 
	 * @param attachmentStatus The status of the attachment
	 */
	public void verifyStatus(String attachmentStatus) {
		if (!StatusCode.CLEAN.equals(attachmentStatus)) {
			throw StatusCode.UNSCANNED.equals(attachmentStatus) || StatusCode.SCANNING.equals(attachmentStatus)
					? AttachmentStatusException.getNotScannedException()
					: AttachmentStatusException.getNotCleanException();
		}
	}

}

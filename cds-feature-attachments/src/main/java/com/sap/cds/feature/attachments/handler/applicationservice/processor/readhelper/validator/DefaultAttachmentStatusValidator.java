/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;

public class DefaultAttachmentStatusValidator implements AttachmentStatusValidator {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAttachmentStatusValidator.class);

	@Override
	public void verifyStatus(String attachmentStatus) {
		if (!StatusCode.CLEAN.equals(attachmentStatus) && !StatusCode.NO_SCANNER.equals(attachmentStatus)) {
			throw StatusCode.UNSCANNED.equals(
					attachmentStatus) ? AttachmentStatusException.getNotScannedException() : AttachmentStatusException.getNotCleanException();
		} else if (StatusCode.NO_SCANNER.equals(attachmentStatus)) {
			logger.warn("No malware scanned used for scanning attachments. Do NOT use this in production.");
		}
	}

}

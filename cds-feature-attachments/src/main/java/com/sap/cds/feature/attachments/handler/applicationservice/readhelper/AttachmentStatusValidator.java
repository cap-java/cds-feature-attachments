/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;

/**
 * The class {@link AttachmentStatusValidator} is responsible for validating the status of an attachment.
 * It checks if the attachment status is CLEAN, and throws an exception if it is not.
 */
public class AttachmentStatusValidator {

	/**
	 * Verifies the status of an attachment.
	 *
	 * @param attachmentStatus the status of the attachment to verify
	 * @throws AttachmentStatusException if the attachment status is not CLEAN
	 */
	public void verifyStatus(String attachmentStatus) throws AttachmentStatusException {
		if (!StatusCode.CLEAN.equals(attachmentStatus)) {
			throw StatusCode.UNSCANNED.equals(attachmentStatus) || StatusCode.SCANNING.equals(
					attachmentStatus) ? AttachmentStatusException.getNotScannedException() : AttachmentStatusException.getNotCleanException();
		}
	}

}

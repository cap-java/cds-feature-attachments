/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.model.service;

/**
 * The class {@link AttachmentModificationResult} is used to store the result of the attachment modification.
 * 
 * @param isInternalStored Indicates if the attachment is stored internally or externally.
 * @param contentId        The content id of the attachment.
 * @param attachmentStatus The status of the attachment.
 */
public record AttachmentModificationResult(boolean isInternalStored, String contentId, String attachmentStatus) {
}

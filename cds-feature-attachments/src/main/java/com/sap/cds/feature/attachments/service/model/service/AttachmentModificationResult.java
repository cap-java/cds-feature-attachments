/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.service.model.service;

/**
 * This record is used to store the result of the attachment modification.
 * 
 * @param isInternalStored Indicates if the attachment is stored internally (in DB) or externally.
 * @param contentId        The content id of the attachment.
 * @param status           The status of the attachment.
 */
public record AttachmentModificationResult(boolean isInternalStored, String contentId, String status) {
}

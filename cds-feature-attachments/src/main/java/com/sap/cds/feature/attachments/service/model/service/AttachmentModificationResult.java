package com.sap.cds.feature.attachments.service.model.service;

/**
	* The class {@link AttachmentModificationResult} is used to store the result of the attachment modification.
	*/
public record AttachmentModificationResult(boolean isInternalStored, String documentId, String attachmentStatus) {
}

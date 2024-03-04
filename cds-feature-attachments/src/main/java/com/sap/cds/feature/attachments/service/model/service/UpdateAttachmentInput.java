package com.sap.cds.feature.attachments.service.model.service;

import java.io.InputStream;

public record UpdateAttachmentInput(String documentId, String attachmentId, String attachmentEntityName,
																																				String fileName, String mimeType, InputStream content) {
}

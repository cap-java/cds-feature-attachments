package com.sap.cds.feature.attachments.service.model.service;

import java.io.InputStream;
import java.util.Map;

/**
	* The class {@link CreateAttachmentInput} is used to store the input for creating an attachment.
	*/
public record CreateAttachmentInput(Map<String, Object> attachmentIds, String attachmentEntityName, String fileName,
																																				String mimeType,
																																				InputStream content) {
}

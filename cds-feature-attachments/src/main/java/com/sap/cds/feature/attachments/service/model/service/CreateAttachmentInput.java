package com.sap.cds.feature.attachments.service.model.service;

import java.io.InputStream;
import java.util.Map;

public record CreateAttachmentInput(Map<String, Object> attachmentIds, String attachmentEntityName, String fileName,
																																				String mimeType,
																																				InputStream content) {
}

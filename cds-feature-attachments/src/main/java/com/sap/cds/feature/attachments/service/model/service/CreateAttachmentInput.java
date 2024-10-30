/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.model.service;

import java.io.InputStream;
import java.util.Map;

import com.sap.cds.reflect.CdsEntity;

/**
 * The class {@link CreateAttachmentInput} is used to store the input for creating an attachment.
 * 
 * @param attachmentIds    The keys for the attachment entity
 * @param attachmentEntity The {@link CdsEntity entity} in which the attachment will be stored
 * @param fileName         The file name of the content
 * @param mimeType         The mime type of the content
 * @param content          The input stream of the content
 */
public record CreateAttachmentInput(Map<String, Object> attachmentIds, CdsEntity attachmentEntity, String fileName,
		String mimeType, InputStream content) {
}

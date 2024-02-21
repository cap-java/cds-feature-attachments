package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.DocumentUploadResult;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.Handler;

/**
 * The interface defining the consumption API of the {@link AttachmentService}.
 * The {@link AttachmentService} is the connection to the storage for attachments, providing extensibility via {@link Handler}.
 */
public interface AttachmentService extends CqnService {

  /**
   * The {@link AttachmentService} wrapping the primary Attachment storage uses this name
   */
  String DEFAULT_NAME = "AttachmentService$Default";

  InputStream readAttachment(String documentId) throws AttachmentAccessException;

  DocumentUploadResult uploadAttachment(String parentId, String fileName, String mimeType, InputStream content) throws AttachmentAccessException;

}

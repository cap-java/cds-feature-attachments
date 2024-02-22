package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.services.Service;

/**
 * The {@link AttachmentService} is the connection to the storage for attachments.
 */
public interface AttachmentService extends Service {

  /**
   * The {@link AttachmentService} wrapping the primary Attachment storage uses this name
   */
  String DEFAULT_NAME = "AttachmentService$Default";

  /**
   * This event is emitted when a document shall be uploaded
   */
  String EVENT_STORE_ATTACHMENT = "STORE_ATTACHMENT";

  /**
   * This event is emitted when a document shall be uploaded
   */
  String EVENT_READ_ATTACHMENT = "READ_ATTACHMENT";

  /**
   * Reads attachment based on the given attachment id
   *
   * @param context Contains the attachment id of the attachment which shall be read
   * @return Returns an input stream of the attachment content
   */
  InputStream readAttachment(AttachmentReadEventContext context) throws AttachmentAccessException;

  /**
   * Stores a document with the given parameter
   *
   * @param context
   * @return
   * @throws AttachmentAccessException
   */
  AttachmentStorageResult storeAttachment(AttachmentStoreEventContext context) throws AttachmentAccessException;

}

package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
import com.sap.cds.services.Service;

/**
 * The {@link AttachmentService} is the connection to the storage for attachments.
 */
public interface AttachmentService extends Service {

  /**
   * The {@link AttachmentService} wrapping the primary attachment storage uses this name
   */
  String DEFAULT_NAME = "AttachmentService$Default";

  /**
   * This event is emitted when an attachment shall be uploaded
   */
  String EVENT_STORE_ATTACHMENT = "STORE_ATTACHMENT";

  /**
   * This event is emitted when an attachment shall be updated
   */
  String EVENT_UPDATE_ATTACHMENT = "UPDATE_ATTACHMENT";

  /**
   * This event is emitted when an attachment shall be uploaded
   */
  String EVENT_READ_ATTACHMENT = "READ_ATTACHMENT";

  /**
   * This event is emitted when an attachment shall be uploaded
   */
  String EVENT_DELETE_ATTACHMENT = "DELETE_ATTACHMENT";

  /**
   * Reads attachment based on the given attachment id
   *
   * @param context Contains the attachment id of the attachment which shall be read
   * @return Returns an input stream of the attachment content
   * @throws AttachmentAccessException Exception to be thrown in case of errors during accessing the attachment
   */
  InputStream readAttachment(AttachmentReadEventContext context) throws AttachmentAccessException;

  /**
   * Stores a document with the given parameter
   *
   * @param context Contains needed data to store the attachment like
   *                - attachmentId
   *                - fileName
   *                - mimeType
   *                - content (mandatory)
   * @return the result of the storage, including the fields:
   *         - fileName
   *         - mimeType
   *         - documentId
   * @throws AttachmentAccessException Exception to be thrown in case of errors during accessing the attachment
   */
  AttachmentStorageResult storeAttachment(AttachmentStoreEventContext context) throws AttachmentAccessException;

  /**
   * Updates a document with the given parameter
   *
   * @param context Contains needed data to update and store the attachment like
   *                - documentId
   *                - attachmentId
   *                - fileName
   *                - mimeType
   *                - content (mandatory)
   * @return the result of the storage, including the fields:
   * 		- fileName
   * 		- mimeType
   * 		- documentId
   * @throws AttachmentAccessException Exception to be thrown in case of errors during accessing the attachment
   */
  AttachmentStorageResult updateAttachment(AttachmentUpdateEventContext context) throws AttachmentAccessException;

  /**
   * Delete an attachment based on the given attachment id
   *
   * @param context Contains the attachment id of the attachment which shall be deleted
   * @throws AttachmentAccessException Exception to be thrown in case of errors during accessing the attachment
   */
  void deleteAttachment(AttachmentDeleteEventContext context) throws AttachmentAccessException;

}

/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service;

import java.io.InputStream;
import java.time.Instant;

import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.services.Service;
import com.sap.cds.services.ServiceException;

/**
 * The {@link AttachmentService} is the connection to the storage for attachments.
 */
public interface AttachmentService extends Service {

	/**
	 * The {@link AttachmentService} wrapping the primary attachment storage uses this name
	 */
	String DEFAULT_NAME = "AttachmentService$Default";

	/**
	 * This event is emitted when an attachment shall be created
	 */
	String EVENT_CREATE_ATTACHMENT = "CREATE_ATTACHMENT";

	/**
	 * This event is emitted when an attachment shall be read
	 */
	String EVENT_READ_ATTACHMENT = "READ_ATTACHMENT";

	/**
	 * This event is emitted when an attachment shall be marked as deleted
	 */
	String EVENT_MARK_ATTACHMENT_AS_DELETED = "MARK_ATTACHMENT_AS_DELETED";

	/**
	 * This event is emitted when an attachment shall be restored
	 */
	String EVENT_RESTORE_ATTACHMENT = "RESTORE_ATTACHMENT";

	/**
	 * Reads attachment based on the given attachment id
	 *
	 * @param contentId Contains the content id of the attachment which shall be read
	 * @return Returns an {@link InputStream} of the attachment content
	 * @throws ServiceException Exception to be thrown in case of errors during accessing the attachment
	 */
	InputStream readAttachment(String contentId);

	/**
	 * Creates the content with the given parameter
	 *
	 * @param input Contains needed data to store the content like
	 *              <ul>
	 *              <li>attachmentIds - list of keys for attachment entity</li>
	 *              <li>attachmentEntity - cds entity in which the attachment will be stored</li>
	 *              <li>fileName</li>
	 *              <li>mimeType</li>
	 *              <li>content (mandatory)</li>
	 *              </ul>
	 * @return the result of the storage:
	 *         <ul>
	 *         <li>isInternalStored - shows if the content was stored internally, this does not indicate errors, in case
	 *         of errors ServiceException is thrown</li>
	 *         <li>contentId - id of the stored content</li>
	 *         </ul>
	 * @throws ServiceException Exception to be thrown in case of errors during accessing the attachment
	 */
	AttachmentModificationResult createAttachment(CreateAttachmentInput input);

	/**
	 * Marks an attachment as deleted based on the given attachment id The attachment will not be deleted physically,
	 * but marked as deleted
	 *
	 * @param input Contains the content id of the content which shall be deleted and the user input of the user which
	 *              created the deletion request
	 * @throws ServiceException Exception to be thrown in case of errors during accessing the attachment
	 */
	void markAttachmentAsDeleted(MarkAsDeletedInput input);

	/**
	 * Restores content after the given timestamp
	 *
	 * @param restoreTimestamp The timestamp from which the documents shall be restored, every content which was deleted
	 *                         after or equal this timestamp shall be restored created documents after this timestamp
	 *                         needs to be mark as deleted
	 * @throws ServiceException Exception to be thrown in case of errors during processing
	 */
	void restoreAttachment(Instant restoreTimestamp);

}

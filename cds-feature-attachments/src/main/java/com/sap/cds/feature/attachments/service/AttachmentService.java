/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.service;

import java.io.InputStream;
import java.time.Instant;

import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
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
	String EVENT_CREATE_ATTACHMENT = "CREATE_ATTACHMENT";

	/**
		* This event is emitted when an attachment shall be uploaded
		*/
	String EVENT_READ_ATTACHMENT = "READ_ATTACHMENT";

	/**
		* This event is emitted when an attachment shall be marked as deleted
		*/
	String EVENT_MARK_AS_DELETED = "MARK_ATTACHMENT_AS_DELETED";

	/**
		* This event is emitted when an attachment shall be uploaded
		*/
	String EVENT_RESTORE = "RESTORE_ATTACHMENT";

	/**
		* Reads attachment based on the given attachment id
		*
		* @param documentId Contains the document id of the attachment which shall be read
		* @return Returns an input stream of the attachment content
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	InputStream readAttachment(String documentId);

	/**
		* Creates a document with the given parameter
		*
		* @param input Contains needed data to store the document like
		*              - attachmentIds - list of keys for attachment entity
		*              - attachmentEntity - cds entity in which the attachment will be stored
		*              - fileName
		*              - mimeType
		*              - content (mandatory)
		* @return the result of the storage:
		* 		- isInternalStored - shows if the document was stored internally, this does not indicate errors, in case of errors ServiceException is thrown
		* 		- documentId - id of the stored document
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	AttachmentModificationResult createAttachment(CreateAttachmentInput input);

	/**
		* Marks an attachment as deleted based on the given attachment id
		* The attachment will not be deleted physically, but marked as deleted
		*
		* @param documentId The document id of the document which shall be deleted
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	void markAsDeleted(String documentId);

	/**
		* Restores document after the given timestamp
		*
		* @param restoreTimestamp The timestamp from which the documents shall be restored, every document which was deleted after or equal this timestamp shall be restored
		*                         created documents after this timestamp needs to be mark as deleted
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during processing
		*/
	void restore(Instant restoreTimestamp);

}

package com.sap.cds.feature.attachments.service;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
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
		* @param documentId Contains the document id of the attachment which shall be read
		* @return Returns an input stream of the attachment content
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	InputStream readAttachment(String documentId);

	/**
		* Creates a document with the given parameter
		*
		* @param input Contains needed data to store the attachment like
		*                - attachmentId
		*                - attachmentEntityName - full qualified name of the entity in which the attachment will be stored
		*                - fileName
		*                - mimeType
		*                - content (mandatory)
		* @return the result of the storage:
		* 		- isExternalStored - shows if the document was stored externally, this does not indicate errors, in case of errors ServiceException is thrown
		* 		- documentId - id of the stored document
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	AttachmentModificationResult createAttachment(CreateAttachmentInput input);

	/**
		* Updates a document with the given parameter
		*
		* @param input Contains needed data to update and store the attachment like
		*                - documentId
		*                - attachmentId
		*                - attachmentEntityName - full qualified name of the entity in which the attachment will be stored
		*                - fileName
		*                - mimeType
		*                - content (mandatory)
		* @return the result of the storage:
		* 		- isExternalStored - shows if the document was stored externally, this does not indicate errors, in case of errors ServiceException is thrown
		* 		- documentId - id of the stored document
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	AttachmentModificationResult updateAttachment(UpdateAttachmentInput input);

	/**
		* Delete an attachment based on the given attachment id
		*
		* @param documentId The document id of the document which shall be deleted
		* @throws com.sap.cds.services.ServiceException Exception to be thrown in case of errors during accessing the attachment
		*/
	void deleteAttachment(String documentId);

}

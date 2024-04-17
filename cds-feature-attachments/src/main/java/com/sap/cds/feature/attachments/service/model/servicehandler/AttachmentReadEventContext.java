package com.sap.cds.feature.attachments.service.model.servicehandler;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

/**
	* The {@link AttachmentReadEventContext} is used to store the context of the read attachment event.
	*/
@EventName(AttachmentService.EVENT_READ_ATTACHMENT)
public interface AttachmentReadEventContext extends EventContext {

	/**
		* Creates an {@link EventContext} already overlayed with this interface. The event is set to be
		* {@link AttachmentService#EVENT_READ_ATTACHMENT}
		*
		* @return the {@link AttachmentReadEventContext}
		*/
	static AttachmentReadEventContext create() {
		return EventContext.create(AttachmentReadEventContext.class, null);
	}

	/**
		* @return The data of the document. The data contain the following fields:
		* 		- content
		* 		- mimeType
		* 		- fileName
		*/
	MediaData getData();

	/**
		* Sets the data of the attachment to be read
		*
		* @param data The data of the document
		*/
	void setData(MediaData data);

	/**
		* @return The ID of the document or {@code null} if no id was specified
		*/
	String getDocumentId();

	/**
		* Sets the document id for the attachment to be read
		*
		* @param documentId The ID of the document
		*/
	void setDocumentId(String documentId);

}

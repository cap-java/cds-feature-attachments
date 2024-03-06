package com.sap.cds.feature.attachments.service.model;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

@EventName(AttachmentService.EVENT_DELETE_ATTACHMENT)
public interface AttachmentDeleteEventContext extends EventContext {

	/**
		* Creates an {@link EventContext} already overlayed with this interface. The event is set to be
		* {@link AttachmentService#EVENT_DELETE_ATTACHMENT}
		*
		* @return the {@link AttachmentDeleteEventContext}
		*/
	static AttachmentDeleteEventContext create() {
		return EventContext.create(AttachmentDeleteEventContext.class, null);
	}

	/**
		* @return The document id for the attachment to be deleted or {@code null} if no id was specified
		*/
	String getDocumentId();

	/**
		* Sets the attachment id for the attachment to be deleted
		*
		* @param documentId The document id of the attachment to be deleted
		*/
	void setDocumentId(String documentId);

}

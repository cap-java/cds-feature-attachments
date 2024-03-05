package com.sap.cds.feature.attachments.service.model.servicehandler;

import java.util.Map;

import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

@EventName(AttachmentService.EVENT_CREATE_ATTACHMENT)
public interface AttachmentCreateEventContext extends EventContext {

	/**
		* Creates an {@link EventContext} already overlayed with this interface. The event is set to be
		* {@link AttachmentService#EVENT_CREATE_ATTACHMENT}
		*
		* @return the {@link AttachmentCreateEventContext}
		*/
	static AttachmentCreateEventContext create() {
		return EventContext.create(AttachmentCreateEventContext.class, null);
	}

	/**
		* @return The id of the attachment storage entity or {@code null} if no id was specified
		*/
	String getDocumentId();

	/**
		* Sets the id af the document for the attachment storage
		*
		* @param documentId The key of the document
		*/
	void setDocumentId(String documentId);

	/**
		* @return The ids of the attachment storage entity or {@code Collections.emptyMap} if no id was specified
		*/
	Map<String, Object> getAttachmentIds();

	/**
		* Sets the id af the attachment entity for the attachment storage
		*
		* @param ids The key of the attachment entity which defines the content field
		*/
	void setAttachmentIds(Map<String, Object> ids);

	/**
		* Sets the full qualified name af the attachment entity for the attachment storage
		* This name can be used to access the data using the persistence service
		*
		* @param attachmentEntityName The name of the attachment entity which defines the content field
		*/
	void setAttachmentEntityName(String attachmentEntityName);

	/**
		* This name can be used to access the data using the persistence service
		*
		* @return The id of the attachment storage entity or {@code null} if no id was specified
		*/
	String getAttachmentEntityName();

	/**
		* @return The data of the document
		*/
	MediaData getData();

	/**
		* Sets the data of the attachment to be read
		*
		* @param data The data of the document
		*/
	void setData(MediaData data);

}

package com.sap.cds.feature.attachments.service.model;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

@EventName(AttachmentService.EVENT_UPDATE_ATTACHMENT)
public interface AttachmentUpdateEventContext extends EventContext {

		/**
			* Creates an {@link EventContext} already overlayed with this interface. The event is set to be
			* {@link AttachmentService#EVENT_UPDATE_ATTACHMENT}
			*
			* @return the {@link AttachmentUpdateEventContext}
			*/
		static AttachmentUpdateEventContext create() {
				return EventContext.create(AttachmentUpdateEventContext.class, null);
		}

		/**
			* Sets the id af the document id in the attachment storage which shall be updated
			*
			* @param id The key of the attachment entity which defines the content field
			*/
		void setDocumentId(String id);

		/**
			* @return The id of the document or {@code null} if no id was specified
			*/
		String getDocumentId();

		/**
			* Sets the id af the attachment entity for the attachment storage
			*
			* @param id The key of the attachment entity which defines the content field
			*/
		void setAttachmentId(String id);

		/**
			* @return The id of the attachment storage entity or {@code null} if no id was specified
			*/
		String getAttachmentId();

		/**
			* Sets the file name for the attachment storage
			*
			* @param fileName The name of the file which shall be stored
			*/
		void setFileName(String fileName);

		/**
			* @return The file name for the attachment storage or {@code null} if no file name was specified
			*/
		String getFileName();

		/**
			* Sets the mime type for the attachment storage
			*
			* @param mimeType The mime type of the file which shall be stored
			*/
		void setMimeType(String mimeType);

		/**
			* @return The mime type for the attachment storage or {@code null} if no mime type was specified
			*/
		String getMimeType();

		/**
			* Sets the content as stream for the attachment storage
			*
			* @param content The content of the file which shall be stored
			*/
		void setContent(InputStream content);

		/**
			* @return The content for the attachment storage or {@code null} if no content was specified
			*/
		InputStream getContent();

}

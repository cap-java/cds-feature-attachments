package com.sap.cds.feature.attachments.service.model;

import java.io.InputStream;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

@EventName(AttachmentService.EVENT_STORE_ATTACHMENT)
public interface AttachmentStoreEventContext extends EventContext {

		/**
			* Creates an {@link EventContext} already overlayed with this interface. The event is set to be
			* {@link AttachmentService#EVENT_STORE_ATTACHMENT}
			*
			* @return the {@link AttachmentStoreEventContext}
			*/
		static AttachmentStoreEventContext create() {
				return EventContext.create(AttachmentStoreEventContext.class, null);
		}

		/**
			* Sets the parent id for the attachment storage
			*
			* @param parentId The key of the parent entity of the entity which defines the content field
			*/
		void setParentId(String parentId);

		/**
			* @return The parent id for the attachment storage or {@code null} if no parent id was specified
			*/
		String getParentId();

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

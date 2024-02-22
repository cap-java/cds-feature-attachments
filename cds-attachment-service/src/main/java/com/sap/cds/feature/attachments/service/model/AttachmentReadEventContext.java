package com.sap.cds.feature.attachments.service.model;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

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
			* @return The attachment id for the attachment to be read or {@code null} if no attachment id was specified
			*/
		String getAttachmentId();

		/**
			* Sets the attachment id for the attachment to be read
			*
			* @param attachmentId The attachment id of the attachment to be read
			*/
		void setAttachmentId(String attachmentId);

}

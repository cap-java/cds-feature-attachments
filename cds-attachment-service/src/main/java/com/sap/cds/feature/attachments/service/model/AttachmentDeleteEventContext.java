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
			* @return The attachment id for the attachment to be deleted or {@code null} if attachment id was specified
			*/
		String getAttachmentId();

		/**
			* Sets the attachment id for the attachment to be deleted
			*
			* @param attachmentId The attachment id of the attachment to be deleted
			*/
		void setAttachmentId(String attachmentId);

}

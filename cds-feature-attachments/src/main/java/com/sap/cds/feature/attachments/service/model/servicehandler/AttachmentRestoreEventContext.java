/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.service.model.servicehandler;

import java.time.Instant;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

/**
	* The {@link AttachmentRestoreEventContext} is used to store the context of the restore deleted attachment event.
	*/
@EventName(AttachmentService.EVENT_RESTORE_ATTACHMENT)
public interface AttachmentRestoreEventContext extends EventContext {

	/**
		* Creates an {@link EventContext} already overlay with this interface. The event is set to be
		* {@link AttachmentService#EVENT_RESTORE_ATTACHMENT}
		*
		* @return the {@link AttachmentRestoreEventContext}
		*/
	static AttachmentRestoreEventContext create() {
		return EventContext.create(AttachmentRestoreEventContext.class, null);
	}

	/**
		* @return The restore timestamp for the contents to be restored or {@code null} if no timestamp was specified
		*/
	Instant getRestoreTimestamp();

	/**
		* Sets the restore timestamp for the contents to be restored
		*
		* @param restoreTimestamp The timestamp from which the contents shall be restored, every content which was deleted after or equal this timestamp will be restored
		*/
	void setRestoreTimestamp(Instant restoreTimestamp);

}

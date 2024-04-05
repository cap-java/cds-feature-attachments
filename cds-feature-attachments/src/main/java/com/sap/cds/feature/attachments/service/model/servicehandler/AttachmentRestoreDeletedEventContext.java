package com.sap.cds.feature.attachments.service.model.servicehandler;

import java.time.Instant;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.EventName;

/**
	* The {@link AttachmentRestoreDeletedEventContext} is used to store the context of the restore deleted attachment event.
	*/
@EventName(AttachmentService.EVENT_RESTORE_DELETED)
public interface AttachmentRestoreDeletedEventContext extends EventContext {

	/**
		* Creates an {@link EventContext} already overlayed with this interface. The event is set to be
		* {@link AttachmentService#EVENT_RESTORE_DELETED}
		*
		* @return the {@link AttachmentRestoreDeletedEventContext}
		*/
	static AttachmentRestoreDeletedEventContext create() {
		return EventContext.create(AttachmentRestoreDeletedEventContext.class, null);
	}

	/**
		* @return The restore timestamp for the documents to be restored or {@code null} if no timestamp was specified
		*/
	Instant getRestoreTimestamp();

	/**
		* Sets the restore timestamp for the documents to be restored
		*
		* @param restoreTimestamp The timestamp from which the documents shall be restored, every document which was deleted after or equal this timestamp will be restored
		*/
	void setRestoreTimestamp(Instant restoreTimestamp);

}

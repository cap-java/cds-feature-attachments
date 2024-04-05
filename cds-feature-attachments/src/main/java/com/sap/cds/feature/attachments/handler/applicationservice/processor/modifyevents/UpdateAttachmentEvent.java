package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
	* The class {@link UpdateAttachmentEvent} is an implementation of the {@link ModifyAttachmentEvent}.
	* The class is used to update an attachment by calling the mark as deleted and create method
	* of the attachment service {@link AttachmentService}.
	* To call these attachment service events the class calls the delete and create event implementation.
	*/
public class UpdateAttachmentEvent implements ModifyAttachmentEvent {

	private final ModifyAttachmentEvent createAttachmentEvent;
	private final ModifyAttachmentEvent deleteAttachmentEvent;

	public UpdateAttachmentEvent(ModifyAttachmentEvent createAttachmentEvent, ModifyAttachmentEvent deleteAttachmentEvent) {
		this.createAttachmentEvent = createAttachmentEvent;
		this.deleteAttachmentEvent = deleteAttachmentEvent;
	}

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		deleteAttachmentEvent.processEvent(path, value, existingData, eventContext);
		return createAttachmentEvent.processEvent(path, value, existingData, eventContext);
	}

}

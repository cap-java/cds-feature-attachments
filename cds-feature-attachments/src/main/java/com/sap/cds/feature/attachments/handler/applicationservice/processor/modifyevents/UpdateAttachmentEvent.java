package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

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

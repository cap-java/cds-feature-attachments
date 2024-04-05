package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
	* The class {@link DoNothingAttachmentEvent} does nothing.
	* The event factory uses this class to create an event that does nothing.
	*/
public class DoNothingAttachmentEvent implements ModifyAttachmentEvent {

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		return value;
	}

}

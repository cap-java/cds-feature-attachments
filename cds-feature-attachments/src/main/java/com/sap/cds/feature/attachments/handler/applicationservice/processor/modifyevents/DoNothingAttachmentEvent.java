package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.services.EventContext;

public class DoNothingAttachmentEvent implements ModifyAttachmentEvent {

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds, EventContext eventContext) {
		return value;
	}

}

package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.services.EventContext;

public class UpdateAttachmentEvent implements ModifyAttachmentEvent {

	private final ModifyAttachmentEvent createAttachmentEvent;
	private final ModifyAttachmentEvent deleteAttachmentEvent;

	public UpdateAttachmentEvent(ModifyAttachmentEvent createAttachmentEvent, ModifyAttachmentEvent deleteAttachmentEvent) {
		this.createAttachmentEvent = createAttachmentEvent;
		this.deleteAttachmentEvent = deleteAttachmentEvent;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds, EventContext eventContext) {
		deleteAttachmentEvent.processEvent(path, element, value, existingData, attachmentIds, eventContext);
		return createAttachmentEvent.processEvent(path, element, value, existingData, attachmentIds, eventContext);
	}

}

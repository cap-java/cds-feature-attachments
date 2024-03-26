package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class UpdateAttachmentEvent implements ModifyAttachmentEvent {

	private final ModifyAttachmentEvent createAttachmentEvent;
	private final ModifyAttachmentEvent deleteAttachmentEvent;

	public UpdateAttachmentEvent(ModifyAttachmentEvent createAttachmentEvent, ModifyAttachmentEvent deleteAttachmentEvent) {
		this.createAttachmentEvent = createAttachmentEvent;
		this.deleteAttachmentEvent = deleteAttachmentEvent;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds) {
		deleteAttachmentEvent.processEvent(path, element, value, existingData, attachmentIds);
		return createAttachmentEvent.processEvent(path, element, value, existingData, attachmentIds);
	}

}

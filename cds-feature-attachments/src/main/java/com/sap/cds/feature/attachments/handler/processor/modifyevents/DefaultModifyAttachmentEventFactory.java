package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.ApplicationHandlerHelper;
import com.sap.cds.services.cds.CqnService;

public class DefaultModifyAttachmentEventFactory implements ModifyAttachmentEventFactory {

	private final ModifyAttachmentEvent createEvent;
	private final ModifyAttachmentEvent updateEvent;
	private final ModifyAttachmentEvent deleteContentEvent;

	public DefaultModifyAttachmentEventFactory(ModifyAttachmentEvent createEvent, ModifyAttachmentEvent updateEvent, ModifyAttachmentEvent deleteContentEvent) {
		this.createEvent = createEvent;
		this.updateEvent = updateEvent;
		this.deleteContentEvent = deleteContentEvent;
	}

	@Override
	public ModifyAttachmentEvent getEvent(String event, Object value, CdsData existingData) {
		if (CqnService.EVENT_UPDATE.equals(event) || CqnService.EVENT_CREATE.equals(event)) {
			if (Objects.isNull(value)) {
				return deleteContentEvent;
			}
			if (ApplicationHandlerHelper.doesDocumentIdExistsBefore(existingData)) {
				return updateEvent;
			} else {
				return createEvent;
			}
		} else {
			throw new IllegalStateException("Unexpected event name: " + event);
		}
	}

}

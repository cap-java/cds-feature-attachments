package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.processor.common.ProcessingBase;

public class DefaultModifyAttachmentEventFactory extends ProcessingBase implements ModifyAttachmentEventFactory {

	private final ModifyAttachmentEvent createEvent;
	private final ModifyAttachmentEvent updateEvent;
	private final ModifyAttachmentEvent deleteContentEvent;
	private final ModifyAttachmentEvent doNothingEvent;

	public DefaultModifyAttachmentEventFactory(ModifyAttachmentEvent createEvent, ModifyAttachmentEvent updateEvent, ModifyAttachmentEvent deleteContentEvent, ModifyAttachmentEvent doNothingEvent) {
		this.createEvent = createEvent;
		this.updateEvent = updateEvent;
		this.deleteContentEvent = deleteContentEvent;
		this.doNothingEvent = doNothingEvent;
	}

	@Override
	public ModifyAttachmentEvent getEvent(Object content, String documentId, boolean documentIdExist, CdsData existingData) {
		var existingDocumentId = existingData.get(Attachments.DOCUMENT_ID);
		if (documentIdExist) {
			if (Objects.isNull(documentId) && Objects.isNull(existingDocumentId)) {
				if (Objects.nonNull(content)) {
					return createEvent;
				} else {
					return doNothingEvent;
				}
			}
			if (Objects.isNull(documentId) && Objects.nonNull(existingDocumentId)) {
				if (Objects.nonNull(content)) {
					return updateEvent;
				} else {
					return deleteContentEvent;
				}
			}
			if (documentId.equals(existingDocumentId)) {
				if (Objects.nonNull(content)) {
					return updateEvent;
				} else {
					return doNothingEvent;
				}
			}

		} else {
			if (Objects.nonNull(existingDocumentId)) {
				if (Objects.nonNull(content)) {
					return updateEvent;
				} else {
					return deleteContentEvent;
				}
			} else {
				if (Objects.nonNull(content)) {
					return createEvent;
				} else {
					return doNothingEvent;
				}
			}

		}
		return doNothingEvent;
	}

}

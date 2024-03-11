package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.util.Objects;
import java.util.Optional;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;

public class DefaultModifyAttachmentEventFactory implements ModifyAttachmentEventFactory {

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
		var event = documentIdExist ? handleExistingDocumentId(content, documentId, existingDocumentId) : handleNonExistingDocumentId(content, existingDocumentId);
		return event.orElse(doNothingEvent);
	}

	private Optional<ModifyAttachmentEvent> handleExistingDocumentId(Object content, String documentId, Object existingDocumentId) {
		ModifyAttachmentEvent event = null;
		if (Objects.isNull(documentId) && Objects.isNull(existingDocumentId) && Objects.nonNull(content)) {
			event = createEvent;
		}
		if (Objects.isNull(documentId) && Objects.nonNull(existingDocumentId)) {
			if (Objects.nonNull(content)) {
				event = updateEvent;
			} else {
				event = deleteContentEvent;
			}
		}
		if (Objects.nonNull(documentId) && documentId.equals(existingDocumentId) && Objects.nonNull(content)) {
			event = updateEvent;
		}
		return Optional.ofNullable(event);
	}

	private Optional<ModifyAttachmentEvent> handleNonExistingDocumentId(Object content, Object existingDocumentId) {
		ModifyAttachmentEvent event = null;
		if (Objects.nonNull(existingDocumentId)) {
			if (Objects.nonNull(content)) {
				event = updateEvent;
			} else {
				event = deleteContentEvent;
			}
		} else {
			if (Objects.nonNull(content)) {
				event = createEvent;
			}
		}
		return Optional.ofNullable(event);
	}

}

package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;

//TODO rename to mark as deleted

/**
	* The class {@link DeleteContentAttachmentEvent} handles the mark of deletion of an attachment.
	* It calls the {@link AttachmentService} to mark the attachment as deleted.
	*/
public class DeleteContentAttachmentEvent implements ModifyAttachmentEvent {

	private static final Logger logger = LoggerFactory.getLogger(DeleteContentAttachmentEvent.class);

	private final AttachmentService outboxedAttachmentService;

	public DeleteContentAttachmentEvent(AttachmentService outboxedAttachmentService) {
		this.outboxedAttachmentService = outboxedAttachmentService;
	}

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		var qualifiedName = eventContext.getTarget().getQualifiedName();
		logger.debug("Processing the event for calling attachment service with mark as delete event for entity {}", qualifiedName);

		if (ApplicationHandlerHelper.doesDocumentIdExistsBefore(existingData) && !DraftService.EVENT_DRAFT_PATCH.equals(eventContext.getEvent())) {
			logger.debug("Calling attachment service with mark as delete event for entity {}", qualifiedName);
			outboxedAttachmentService.markAsDeleted((String) existingData.get(Attachments.DOCUMENT_ID));
		} else {
			logger.debug("Do NOT call attachment service with mark as delete event for entity {} as no document id found in existing data and event is DRAFT_PATCH event", qualifiedName);
		}
		if (Objects.nonNull(path)) {
			var newDocumentId = path.target().values().get(Attachments.DOCUMENT_ID);
			if (Objects.nonNull(newDocumentId) && newDocumentId.equals(existingData.get(Attachments.DOCUMENT_ID)) || !path.target()
																																																																																																															.values()
																																																																																																															.containsKey(Attachments.DOCUMENT_ID)) {
				path.target().values().put(Attachments.DOCUMENT_ID, null);
			}
		}
		return value;
	}

}

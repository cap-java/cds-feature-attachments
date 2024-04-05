package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;

//TODO add javadoc
//TODO rename to mark as deleted
public class DeleteContentAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService outboxedAttachmentService;

	public DeleteContentAttachmentEvent(AttachmentService outboxedAttachmentService) {
		this.outboxedAttachmentService = outboxedAttachmentService;
	}

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		if (ApplicationHandlerHelper.doesDocumentIdExistsBefore(existingData) && !DraftService.EVENT_DRAFT_PATCH.equals(eventContext.getEvent())) {
			outboxedAttachmentService.markAsDeleted((String) existingData.get(Attachments.DOCUMENT_ID));
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

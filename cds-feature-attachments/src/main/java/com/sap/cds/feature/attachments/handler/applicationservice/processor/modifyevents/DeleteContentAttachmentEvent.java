package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

public class DeleteContentAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService outboxedAttachmentService;

	public DeleteContentAttachmentEvent(AttachmentService outboxedAttachmentService) {
		this.outboxedAttachmentService = outboxedAttachmentService;
	}

	@Override
	public Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext) {
		if (ApplicationHandlerHelper.doesDocumentIdExistsBefore(existingData)) {
			outboxedAttachmentService.markAsDeleted((String) existingData.get(Attachments.DOCUMENT_ID));
		}
		path.target().values().put(Attachments.DOCUMENT_ID, null);
		return value;
	}

}

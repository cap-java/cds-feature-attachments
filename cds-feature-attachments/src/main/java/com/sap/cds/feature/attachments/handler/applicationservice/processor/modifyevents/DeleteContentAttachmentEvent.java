package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class DeleteContentAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;

	public DeleteContentAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds) {
		if (ApplicationHandlerHelper.doesDocumentIdExistsBefore(existingData)) {
			attachmentService.deleteAttachment((String) existingData.get(Attachments.DOCUMENT_ID));
		}
		path.target().values().put(Attachments.DOCUMENT_ID, null);
		return value;
	}

}

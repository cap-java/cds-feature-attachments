package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.processor.common.ProcessingBase;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class DeleteContentAttachmentEvent extends ProcessingBase implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;

	public DeleteContentAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds) {
		if (doesDocumentIdExistsBefore(existingData)) {
			attachmentService.deleteAttachment((String) existingData.get(Attachments.DOCUMENT_ID));
		}
		path.target().values().put(Attachments.DOCUMENT_ID, null);
		return value;
	}

}

package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.io.InputStream;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class UpdateAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;

	public UpdateAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds) {
		var values = path.target().values();

		var mimeTypeOptional = ModifyAttachmentEventHelper.getFieldValue(MediaData.MIME_TYPE, values, existingData);
		var fileNameOptional = ModifyAttachmentEventHelper.getFieldValue(MediaData.FILE_NAME, values, existingData);
		var documentId = (String) existingData.get(Attachments.DOCUMENT_ID);

		var input = new UpdateAttachmentInput(documentId, attachmentIds, path.target().entity().getQualifiedName(), fileNameOptional.orElse(null), mimeTypeOptional.orElse(null), (InputStream) value);
		var result = attachmentService.updateAttachment(input);
		path.target().values().put(Attachments.DOCUMENT_ID, result.documentId());
		return result.isExternalStored() ? null : value;
	}

}

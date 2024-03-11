package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.io.InputStream;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class CreateAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;

	public CreateAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, Object value, CdsData existingData, Map<String, Object> attachmentIds) {
		var values = path.target().values();
		var mimeTypeOptional = ModifyAttachmentEventHelper.getFieldValue(MediaData.MIME_TYPE, values, existingData);
		var fileNameOptional = ModifyAttachmentEventHelper.getFieldValue(MediaData.FILE_NAME, values, existingData);

		var createEventInput = new CreateAttachmentInput(attachmentIds, path.target().entity().getQualifiedName(), fileNameOptional.orElse(null), mimeTypeOptional.orElse(null), (InputStream) value);
		var result = attachmentService.createAttachment(createEventInput);
		path.target().values().put(Attachments.DOCUMENT_ID, result.documentId());
		return result.isExternalStored() ? null : value;
	}

}

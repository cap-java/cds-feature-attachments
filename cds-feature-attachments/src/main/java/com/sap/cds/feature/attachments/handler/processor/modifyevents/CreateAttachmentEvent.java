package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.io.InputStream;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class CreateAttachmentEvent extends ModifyAttachmentEventBase implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;

	public CreateAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, AttachmentFieldNames fieldNames, Object value, CdsData existingData, String attachmentId) {
		var values = path.target().values();
		var mimeTypeOptional = getFieldName(fieldNames.mimeTypeField(), values, existingData);
		var fileNameOptional = getFieldName(fieldNames.fileNameField(), values, existingData);

		var createEventInput = new CreateAttachmentInput(attachmentId, path.target().entity().getQualifiedName(), fileNameOptional.orElse(null), mimeTypeOptional.orElse(null), (InputStream) value);
		var result = attachmentService.createAttachment(createEventInput);
		fieldNames.documentIdField().ifPresent(doc -> path.target().values().put(doc, result.documentId()));
		return result.isExternalStored() ? null : value;
	}

}
package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.io.InputStream;
import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class UpdateAttachmentEvent implements ModifyAttachmentEvent {

	private final AttachmentService attachmentService;

	public UpdateAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@Override
	public Object processEvent(Path path, CdsElement element, AttachmentFieldNames fieldNames, Object value, CdsData existingData, String attachmentId) {
		var updateEventContext = AttachmentUpdateEventContext.create();
		updateEventContext.setAttachmentId(attachmentId);

		var values = path.target().values();
		updateEventContext.setContent((InputStream) value);

		var mimeTypeOptional = fieldNames.mimeTypeField().map(anno -> {
			var annotationValue = values.get(anno);
			var mimeType = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(anno);
			return (String) mimeType;
		});

		var fileNameOptional = fieldNames.fileNameField().map(anno -> {
			var annotationValue = values.get(anno);
			var fileName = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(anno);
			return (String) fileName;
		});
		var documentId = fieldNames.documentIdField().map(docId -> (String) existingData.get(docId));

		var input = new UpdateAttachmentInput(documentId.orElse(null), attachmentId, path.target().entity().getQualifiedName(), fileNameOptional.orElse(null), mimeTypeOptional.orElse(null), (InputStream) value);
		var result = attachmentService.updateAttachment(input);
		fieldNames.documentIdField().ifPresent(doc -> path.target().values().put(doc, result.documentId()));
		return result.isExternalStored() ? null : value;
	}

}

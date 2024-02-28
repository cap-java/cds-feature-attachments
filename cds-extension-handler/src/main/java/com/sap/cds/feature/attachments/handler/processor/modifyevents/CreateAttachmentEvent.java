package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.io.InputStream;
import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentCreateEventContext;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class CreateAttachmentEvent implements ModifyAttachmentEvent {

		private final AttachmentService attachmentService;

		public CreateAttachmentEvent(AttachmentService attachmentService) {
				this.attachmentService = attachmentService;
		}

		@Override
		public Object processEvent(Path path, CdsElement element, AttachmentFieldNames fieldNames, Object value, CdsData existingData, String attachmentId) throws AttachmentAccessException {
				var createEventContext = AttachmentCreateEventContext.create();
				createEventContext.setAttachmentId(attachmentId);

				var values = path.target().values();
				createEventContext.setContent((InputStream) value);

				fieldNames.mimeTypeField().ifPresent(anno -> {
						var annotationValue = values.get(anno);
						var mimeType = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(anno);
						createEventContext.setMimeType((String) mimeType);
				});

				fieldNames.fileNameField().ifPresent(anno -> {
						var annotationValue = values.get(anno);
						var fileName = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(anno);
						createEventContext.setFileName((String) fileName);
				});

				var result = attachmentService.createAttachment(createEventContext);
				fieldNames.documentIdField().ifPresent(doc -> path.target().values().put(doc, result.documentId()));
				return result.isExternalStored() ? null : value;
		}

}

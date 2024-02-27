package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.ProcessingBase;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public class DeleteContentAttachmentEvent extends ProcessingBase implements ModifyAttachmentEvent {

		private final AttachmentService attachmentService;

		public DeleteContentAttachmentEvent(AttachmentService attachmentService) {
				this.attachmentService = attachmentService;
		}

		@Override
		public Object processEvent(Path path, CdsElement element, AttachmentFieldNames fieldNames, Object value, CdsData existingData, String attachmentId) throws AttachmentAccessException {
				if (fieldNames.documentIdField().isEmpty()) {
						return value;
				}

				if (doesDocumentIdExistsBefore(fieldNames, existingData)) {
						var deleteContext = AttachmentDeleteEventContext.create();
						deleteContext.setDocumentId((String) existingData.get(fieldNames.documentIdField().get()));
						attachmentService.deleteAttachment(deleteContext);
				}
				path.target().values().put(fieldNames.documentIdField().get(), null);
				return value;
		}

}

package com.sap.cds.feature.attachments.handler.processor.common;

import java.util.Map;
import java.util.Objects;

import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;

public abstract class ProcessingBase {

		protected boolean doesDocumentIdExistsBefore(AttachmentFieldNames fieldNames, Map<?, Object> oldData) {
				return fieldNames.documentIdField().isPresent() && Objects.nonNull(oldData.get(fieldNames.documentIdField().get()));
		}

}

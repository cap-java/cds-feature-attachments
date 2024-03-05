package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sap.cds.CdsData;

abstract class ModifyAttachmentEventBase {

	protected Optional<String> getFieldValue(String fieldName, Map<String, Object> values, CdsData existingData) {
		var annotationValue = values.get(fieldName);
		var value = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(fieldName);
		return Optional.ofNullable((String) value);
	}

}

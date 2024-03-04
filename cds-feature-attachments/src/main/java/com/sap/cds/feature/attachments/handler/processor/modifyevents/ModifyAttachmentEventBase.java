package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sap.cds.CdsData;

abstract class ModifyAttachmentEventBase {

	protected Optional<String> getFieldName(Optional<String> fieldNames, Map<String, Object> values, CdsData existingData) {
		return fieldNames.map(anno -> {
			var annotationValue = values.get(anno);
			var mimeType = Objects.nonNull(annotationValue) ? annotationValue : existingData.get(anno);
			return (String) mimeType;
		});
	}

}

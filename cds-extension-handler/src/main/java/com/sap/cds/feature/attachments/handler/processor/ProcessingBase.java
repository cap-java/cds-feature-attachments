package com.sap.cds.feature.attachments.handler.processor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;

public abstract class ProcessingBase {

		protected void callProcessor(CdsEntity entity, List<CdsData> data, Filter filter, Converter converter) {
				CdsDataProcessor.create().addConverter(
								filter, converter)
						.process(data, entity);
		}

		protected boolean hasElementAnnotation(CdsElement element, String annotation) {
				return element.findAnnotation(annotation).isPresent();
		}

		protected boolean doesDocumentIdExistsBefore(AttachmentFieldNames fieldNames, Map<?, Object> oldData) {
				return fieldNames.documentIdField().isPresent() && Objects.nonNull(oldData.get(fieldNames.documentIdField().get()));
		}


}

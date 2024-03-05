package com.sap.cds.feature.attachments.handler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.processor.common.ProcessingBase;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;

abstract class ApplicationHandlerBase extends ProcessingBase {

	protected boolean isContentFieldInData(CdsEntity entity, List<CdsData> data) {
		var isIncluded = new AtomicBoolean();

		Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)
																																													&& hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
		Converter converter = (path, element, value) -> {
			isIncluded.set(true);
			return value;
		};

		callProcessor(entity, data, filter, converter);
		return isIncluded.get();
	}

	protected void callProcessor(CdsEntity entity, List<CdsData> data, Filter filter, Converter converter) {
		CdsDataProcessor.create().addConverter(
						filter, converter)
				.process(data, entity);
	}

	protected Filter buildFilterForMediaTypeEntity() {
		return (path, element, type) -> isMediaEntity(path.target().type()) && hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
	}

	protected boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

	protected boolean hasElementAnnotation(CdsElement element, String annotation) {
		return element.findAnnotation(annotation).isPresent();
	}

	protected String getIdField(ResolvedSegment target) {
		var targetElements = target.entity().elements().toList();
		return target.keys().keySet().stream().filter(key -> targetElements.stream().anyMatch(elem -> elem.getName().equals(key) && (elem.getType().isSimpleType(CdsBaseType.UUID)))).findAny().orElseThrow();
	}

}

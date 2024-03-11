package com.sap.cds.feature.attachments.handler.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;

public final class ApplicationHandlerHelper {

	private ApplicationHandlerHelper() {
	}

	public static boolean isContentFieldInData(CdsEntity entity, List<CdsData> data) {
		var isIncluded = new AtomicBoolean();

		Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)
																																													&& hasElementAnnotation(element, ModelConstants.ANNOTATION_CORE_MEDIA_TYPE);
		Validator validator = (path, element, value) -> {
			isIncluded.set(true);
		};

		callValidator(entity, data, filter, validator);
		return isIncluded.get();
	}

	public static void callProcessor(CdsEntity entity, List<CdsData> data, Filter filter, Converter converter) {
		CdsDataProcessor.create().addConverter(
						filter, converter)
				.process(data, entity);
	}

	public static void callValidator(CdsEntity entity, List<CdsData> data, Filter filter, Validator validator) {
		CdsDataProcessor.create().addValidator(
						filter, validator)
				.process(data, entity);
	}

	public static Filter buildFilterForMediaTypeEntity() {
		return (path, element, type) -> isMediaEntity(path.target().type()) && hasElementAnnotation(element, ModelConstants.ANNOTATION_CORE_MEDIA_TYPE);
	}

	public static boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

	public static boolean hasElementAnnotation(CdsElement element, String annotation) {
		return element.findAnnotation(annotation).isPresent();
	}

	public static boolean doesDocumentIdExistsBefore(Map<?, Object> existingData) {
		return Objects.nonNull(existingData.get(Attachments.DOCUMENT_ID));
	}

}

package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.common.ProcessingBase;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;

public class ApplicationEventBase extends ProcessingBase {

		private static final Logger logger = LoggerFactory.getLogger(ApplicationEventBase.class);

		protected boolean isContentFieldInData(CdsEntity entity, List<CdsData> data, List<String> processedEntityNames) {
				var isIncluded = new AtomicBoolean();

				Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)
						&& hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
				Converter converter = (path, element, value) -> {
						isIncluded.set(true);
						return value;
				};

				callProcessor(entity, data, filter, converter);
				processedEntityNames.add(entity.getName());

				if (!isIncluded.get()) {
						entity.associations().forEach(element -> {
								var target = element.getType().as(CdsAssociationType.class).getTarget();
								if (!processedEntityNames.contains(target.getName())) {
										var included = isContentFieldInData(element.getType().as(CdsAssociationType.class).getTarget(), data, processedEntityNames);
										isIncluded.set(included);
								}
						});
				}

				return isIncluded.get();
		}

		protected AttachmentFieldNames getFieldNames(CdsElement element, ResolvedSegment target) {
				var attachmentIdField = new AtomicReference<String>();
				target.keys().forEach((key, val) -> attachmentIdField.set(key));

				var documentIdElement = target.type().elements().filter(targetElement -> hasElementAnnotation(targetElement, ModelConstants.ANNOTATION_IS_EXTERNAL_DOCUMENT_ID)).findAny();
				var documentIdField = documentIdElement.map(CdsElementDefinition::getName);
				logEmptyFieldName("document ID", documentIdField);

				var mediaTypeAnnotation = element.findAnnotation(ModelConstants.ANNOTATION_MEDIA_TYPE);
				var fileNameAnnotation = element.findAnnotation(ModelConstants.ANNOTATION_FILE_NAME);

				var mimeTypeField = mediaTypeAnnotation.map(this::getString);
				logEmptyFieldName("mime type", mimeTypeField);
				var fileNameField = fileNameAnnotation.map(this::getString);
				logEmptyFieldName("file name", fileNameField);

				return new AttachmentFieldNames(attachmentIdField.get(), documentIdField, mimeTypeField, fileNameField);
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

		private void logEmptyFieldName(String fieldName, Optional<String> value) {
				if (value.isEmpty()) {
						logger.warn("For Attachments no field for {} was found", fieldName);
				}
		}

		private String getString(CdsAnnotation<Object> anno) {
				if (anno.getValue() instanceof Map<?, ?> annoMap) {
						return (String) annoMap.get("=");
				}
				return null;
		}

}

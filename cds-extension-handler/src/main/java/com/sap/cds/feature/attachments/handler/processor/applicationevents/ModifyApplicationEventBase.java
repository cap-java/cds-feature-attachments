package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.common.ProcessingBase;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

abstract class ModifyApplicationEventBase extends ProcessingBase implements ApplicationEvent {

		private static final Logger logger = LoggerFactory.getLogger(ModifyApplicationEventBase.class);

		private final PersistenceService persistenceService;
		private final ModifyAttachmentEventFactory eventFactory;

		ModifyApplicationEventBase(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
				this.persistenceService = persistenceService;
				this.eventFactory = eventFactory;
		}

		boolean processingNotNeeded(CdsEntity entity, List<CdsData> data) {
				return !isContentFieldInData(entity, data);
		}

		void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event) {
				Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false) && hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
				Converter converter = (path, element, value) -> {
						var fieldNames = getFieldNames(element, path.target());
						var attachmentIdObject = path.target().keys().get(fieldNames.keyField());
						var attachmentId = Objects.nonNull(attachmentIdObject) ? String.valueOf(attachmentIdObject) : null;
						var oldData = CqnService.EVENT_UPDATE.equals(event) ? readExistingData(attachmentId, path.target().entity()) : CdsData.create();

						var eventToProcess = eventFactory.getEvent(event, value, fieldNames, oldData);
						return eventToProcess.processEvent(path, element, fieldNames, value, oldData, attachmentId);
				};
				callProcessor(entity, data, filter, converter);
				entity.associations().forEach(element -> uploadAttachmentForEntity(element.getType().as(CdsAssociationType.class).getTarget(), data, event));
		}

		private boolean isContentFieldInData(CdsEntity entity, List<CdsData> data) {
				var isIncluded = new AtomicBoolean();

				Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)
						&& hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
				Converter converter = (path, element, value) -> {
						isIncluded.set(true);
						return value;
				};

				callProcessor(entity, data, filter, converter);

				if (!isIncluded.get()) {
						entity.associations().forEach(element -> {
								var included = isContentFieldInData(element.getType().as(CdsAssociationType.class).getTarget(), data);
								isIncluded.set(included);
						});
				}

				return isIncluded.get();
		}

		private AttachmentFieldNames getFieldNames(CdsElement element, ResolvedSegment target) {
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

		private CdsData readExistingData(String attachmentId, CdsEntity entity) {
				if (Objects.isNull(attachmentId)) {
						logger.error("no id provided for attachment entity");
						throw new IllegalStateException("no attachment id provided");
				}

				CqnSelect select = Select.from(entity).byId(attachmentId);
				var result = persistenceService.run(select);
				return result.single();
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

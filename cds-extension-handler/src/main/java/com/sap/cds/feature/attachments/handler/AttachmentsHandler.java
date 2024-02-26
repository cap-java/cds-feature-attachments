package com.sap.cds.feature.attachments.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.ApplicationEventProcessor;
import com.sap.cds.feature.attachments.handler.processor.ProcessingBase;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentsHandler extends ProcessingBase implements EventHandler {

		private static final Logger logger = LoggerFactory.getLogger(AttachmentsHandler.class);

		private final PersistenceService persistenceService;
		private final AttachmentService attachmentService;
		private final ApplicationEventProcessor eventProcessor;

		public AttachmentsHandler(PersistenceService persistenceService, AttachmentService attachmentService, ApplicationEventProcessor eventProcessor) {
				this.persistenceService = persistenceService;
				this.attachmentService = attachmentService;
				this.eventProcessor = eventProcessor;
		}

		@After
		@HandlerOrder(HandlerOrder.EARLY)
		void readAttachments(CdsReadEventContext context, List<CdsData> data) throws AttachmentAccessException {

				//fill mimeType for content in case entity with MediaType annotation is requested

				var cdsModel = context.getCdsRuntime().getCdsModel();
				if (data.size() != 1) {  //content is to be read only for single attachment
						return;
				}

				if (context.getTarget().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)) {

						final CqnSelect cqn = context.getCqn();
						// read from attachment service only if content is asked
						if (cqn.items().stream().filter(i -> i.isRef()).map(i -> i.asRef())
								.anyMatch(i -> i.path().equals("content"))) { // for delete this condition is not fulfilled

								var attachmentId = CqnAnalyzer.create(cdsModel).analyze(cqn).targetKeys().get("ID").toString();
								var readContext = AttachmentReadEventContext.create();
								readContext.setDocumentId(attachmentId);
								var content = attachmentService.readAttachment(readContext);
								var existingContent = data.get(0).get("content");
								if (Objects.isNull(existingContent)) {
										data.get(0).put("content", content);
								}
						}
				}
		}

		@After(event = {CqnService.EVENT_DELETE})
		void deleteAttachments(CdsDeleteEventContext context) throws AttachmentAccessException {

				//TODO implement cascading delete e.g. Root is deleted and items -> attachments shall also be deleted

				var cdsModel = context.getCdsRuntime().getCdsModel();

				//check if entity is of type attachment
				if (context.getTarget().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)) {
						final String attachmentId = CqnAnalyzer.create(cdsModel).analyze(context.getCqn()).targetKeys().get("ID").toString();
						var deleteContext = AttachmentDeleteEventContext.create();
						//TODO fill attachment id
						attachmentService.deleteAttachment(deleteContext);

				} else if (context.getTarget().findAssociation("attachments").isPresent()) {
						//check if parent entity has association to attachments
//						final String up_Id = CqnAnalyzer.create(cdsModel).analyze(context.getCqn()).targetKeys().get("ID").toString();
						var deleteContext = AttachmentDeleteEventContext.create();
//						deleteContext.setAttachmentId(); TODO fill attachment id
						attachmentService.deleteAttachment(deleteContext);
				}

		}

		@Before(event = CqnService.EVENT_CREATE)
		void uploadAttachmentsForCreate(CdsCreateEventContext context, List<CdsData> data) {
				if (processingNotNeeded(context.getTarget(), data)) {
						return;
				}
				setKeysInData(context.getTarget(), data);
				uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_CREATE);
		}

		@Before(event = CqnService.EVENT_UPDATE)
		void uploadAttachmentsForUpdate(CdsUpdateEventContext context, List<CdsData> data) {
				if (processingNotNeeded(context.getTarget(), data)) {
						return;
				}
				uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_UPDATE);
		}

		private boolean processingNotNeeded(CdsEntity entity, List<CdsData> data) {
				return !eventProcessor.isAttachmentEvent(entity, data);
		}

		private void setKeysInData(CdsEntity entity, List<CdsData> data) {
				CdsDataProcessor.create().addGenerator(
								(path, element, type) -> path.target().type().keyElements().count() == 1 && element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
								(path, element, isNull) -> UUID.randomUUID().toString())
						.process(data, entity);

				entity.associations().forEach(element -> setKeysInData(element.getType().as(CdsAssociationType.class).getTarget(), data));
		}

		private void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event) {
				Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false) && hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
				Converter converter = (path, element, value) -> {
						try {
								var fieldNames = getFieldNames(element, path.target());
								var attachmentId = (String) path.target().keys().get(fieldNames.keyField());
								var oldData = CqnService.EVENT_UPDATE.equals(event) ? readExistingData(attachmentId, path.target().entity()) : CdsData.create();

								var eventToProcess = eventProcessor.getEvent(event, value, fieldNames, oldData);
								return eventToProcess.processEvent(path, element, fieldNames, value, oldData, attachmentId);

						} catch (AttachmentAccessException e) {
								throw new ServiceException(e);
						}
				};
				callProcessor(entity, data, filter, converter);
				entity.associations().forEach(element -> uploadAttachmentForEntity(element.getType().as(CdsAssociationType.class).getTarget(), data, event));
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

		private String getString(CdsAnnotation<Object> anno) {
				if (anno.getValue() instanceof Map<?, ?> annoMap) {
						return (String) annoMap.get("=");
				}
				return null;
		}

		private void logEmptyFieldName(String fieldName, Optional<String> value) {
				if (value.isEmpty()) {
						logger.warn("For Attachments no field for {} was found", fieldName);
				}
		}

}

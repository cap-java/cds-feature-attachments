package com.sap.cds.feature.attachments.handler;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.ResolvedSegment;
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
public class AttachmentsHandler implements EventHandler {

		//TODO Logging / Error handling

		private final PersistenceService persistenceService;
		private final AttachmentService attachmentService;

		public AttachmentsHandler(PersistenceService persistenceService, AttachmentService attachmentService) {
				this.persistenceService = persistenceService;
				this.attachmentService = attachmentService;
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
				if (!isContentFieldInData(context.getTarget(), data)) {
						return;
				}
				setKeysInData(context.getTarget(), data);
				uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_CREATE);
		}

		@Before(event = CqnService.EVENT_UPDATE)
		void uploadAttachmentsForUpdate(CdsUpdateEventContext context, List<CdsData> data) {
				if (!isContentFieldInData(context.getTarget(), data)) {
						return;
				}
				uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_UPDATE);
		}

		private boolean isContentFieldInData(CdsEntity entity, List<CdsData> data) {
				var isIncluded = new AtomicBoolean();
				//TODO put in different class
				CdsDataProcessor.create().addConverter(
								(path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false) && hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE), // filter
								(path, element, value) -> {
										isIncluded.set(true);
										return value;
								})
						.process(data, entity);

				if (!isIncluded.get()) {
						entity.associations().forEach(element -> {
								var included = isIncluded.get() || isContentFieldInData(element.getType().as(CdsAssociationType.class).getTarget(), data);
								isIncluded.set(included);
						});
				}

				return isIncluded.get();
		}

		private void setKeysInData(CdsEntity entity, List<CdsData> data) {
				CdsDataProcessor.create().addGenerator(
								(path, element, type) -> path.target().type().keyElements().count() == 1 && element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
								(path, element, isNull) -> UUID.randomUUID().toString())
						.process(data, entity);

				entity.associations().forEach(element -> setKeysInData(element.getType().as(CdsAssociationType.class).getTarget(), data));
		}

		private void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event) {
				//TODO refactor
				CdsDataProcessor.create().addConverter(
								(path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false) && hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE), // filter
								(path, element, value) -> {
										try {
												var fieldNames = getFieldNames(element, path.target());

												var oldData = CqnService.EVENT_UPDATE.equals(event) ? readExistingData(fieldNames.keyField(), path.target().entity()) : Collections.emptyMap();

												if (Objects.isNull(value)) {
														if (doesDocumentIdExistsBefore(fieldNames, oldData)) {
																var deleteContext = AttachmentDeleteEventContext.create();
																deleteContext.setDocumentId((String) oldData.get(fieldNames.documentIdField().get()));
																attachmentService.deleteAttachment(deleteContext);
														}
														path.target().values().put(fieldNames.documentIdField().get(), null);
														return value;
												}

												AttachmentStorageResult uploadResult;
												if (doesDocumentIdExistsBefore(fieldNames, oldData)) {
														var storageContext = AttachmentUpdateEventContext.create();
														storageContext.setAttachmentId(fieldNames.keyField());

														var values = path.target().values();
														storageContext.setContent((InputStream) value);

														fieldNames.mimeTypeField().ifPresent(anno -> {
																var annotationValue = values.get(anno);
																var mimeType = Objects.nonNull(annotationValue) ? annotationValue : oldData.get(anno);
																storageContext.setMimeType((String) mimeType);
														});

														fieldNames.fileNameField().ifPresent(anno -> {
																var annotationValue = values.get(anno);
																var fileName = Objects.nonNull(annotationValue) ? annotationValue : oldData.get(anno);
																storageContext.setFileName((String) fileName);
														});
														storageContext.setDocumentId((String) oldData.get(fieldNames.documentIdField().get()));
														uploadResult = attachmentService.updateAttachment(storageContext);
												} else {
														var storageContext = AttachmentStoreEventContext.create();
														storageContext.setAttachmentId(fieldNames.keyField());

														var values = path.target().values();
														storageContext.setContent((InputStream) value);

														fieldNames.mimeTypeField().ifPresent(anno -> {
																var annotationValue = values.get(anno);
																var mimeType = Objects.nonNull(annotationValue) ? annotationValue : oldData.get(anno);
																storageContext.setMimeType((String) mimeType);
														});

														fieldNames.fileNameField().ifPresent(anno -> {
																var annotationValue = values.get(anno);
																var fileName = Objects.nonNull(annotationValue) ? annotationValue : oldData.get(anno);
																storageContext.setFileName((String) fileName);
														});

														uploadResult = attachmentService.storeAttachment(storageContext);
												}
												fieldNames.documentIdField().ifPresent(doc -> path.target().values().put(doc, uploadResult.documentId()));

												return uploadResult.isExternalStored() ? null : value;
										} catch (AttachmentAccessException e) {
												throw new ServiceException(e);
										}
								})
						.process(data, entity);

				entity.associations().forEach(element -> uploadAttachmentForEntity(element.getType().as(CdsAssociationType.class).getTarget(), data, event));
		}

		private boolean doesDocumentIdExistsBefore(AttachmentFieldNames fieldNames, Map<?, Object> oldData) {
				return fieldNames.documentIdField().isPresent() && Objects.nonNull(oldData.get(fieldNames.documentIdField().get()));
		}

		private CdsData readExistingData(String attachmentId, CdsEntity entity) {
				//TODO error log if id empty
				CqnSelect select = Select.from(entity).byId(attachmentId);
				var result = persistenceService.run(select);
				//TODO error log if result not found
				return result.single();
		}

		private boolean hasElementAnnotation(CdsElement element, String annotation) {
				return element.findAnnotation(annotation).isPresent();
		}

		private AttachmentFieldNames getFieldNames(CdsElement element, ResolvedSegment target) {
				var attachmentIdField = new AtomicReference<String>();
				target.keys().forEach((key, val) -> attachmentIdField.set((String) val));

				var documentIdElement = target.type().elements().filter(targetElement -> hasElementAnnotation(targetElement, ModelConstants.ANNOTATION_IS_EXTERNAL_DOCUMENT_ID)).findAny();
				var documentIdField = documentIdElement.map(CdsElementDefinition::getName);

				var mediaTypeAnnotation = element.findAnnotation(ModelConstants.ANNOTATION_MEDIA_TYPE);
				var fileNameAnnotation = element.findAnnotation(ModelConstants.ANNOTATION_FILE_NAME);

				//TODO validate ann.getValue before cast
				var mimeTypeField = mediaTypeAnnotation.map(anno -> ((Map<String, String>) anno.getValue()).get("="));
				var fileNameField = fileNameAnnotation.map(anno -> ((Map<String, String>) anno.getValue()).get("="));

				return new AttachmentFieldNames(attachmentIdField.get(), documentIdField, mimeTypeField, fileNameField);
		}

}

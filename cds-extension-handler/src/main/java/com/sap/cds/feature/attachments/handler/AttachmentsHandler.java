package com.sap.cds.feature.attachments.handler;

import java.util.List;
import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentsHandler implements EventHandler {

		//TODO Logging

		private final AttachmentService attachmentService;

		public AttachmentsHandler(AttachmentService attachmentService) {
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
								readContext.setAttachmentId(attachmentId);
								var content = attachmentService.readAttachment(readContext);
								data.get(0).put("content", content);
						}
				}
		}

		@After(event = {CqnService.EVENT_DELETE})
		void deleteAttachments(CdsDeleteEventContext context) throws AttachmentAccessException {

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

		@Before(event = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void uploadAttachments(EventContext context, List<CdsData> data) {
				uploadAttachmentForEntity(context.getTarget(), data);
		}

		private void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data) {
				var mediaAnnotation = ModelConstants.ANNOTATION_IS_MEDIA_DATA;
				var contentFieldAnnotation = ModelConstants.ANNOTATION_MEDIA_TYPE;
				CdsDataProcessor.create().addConverter(
								(path, element, type) -> path.target().type().getAnnotationValue(mediaAnnotation, false) && element.getAnnotationValue(contentFieldAnnotation, false), // filter
								(path, element, value) -> {
										AttachmentStorageResult uploadResult = null;
										try {
//									final String fileName = path.target();
												final String parentId = path.root().keys().get("ID").toString();
												var storageContext = AttachmentStoreEventContext.create();
												// TODO fill context
												uploadResult = attachmentService.storeAttachment(storageContext);
										} catch (AttachmentAccessException e) {
												throw new ServiceException(e);
										}
										return Objects.nonNull(uploadResult) ? uploadResult.documentId() : null; //convert value of content to null in db; attachmentService should return null if it is not the default service (i.e. s3 or sdm)
								})
						.process(data, entity);

				entity.associations().forEach(element -> uploadAttachmentForEntity(element.getType().as(CdsAssociationType.class).getTarget(), data));
		}

}

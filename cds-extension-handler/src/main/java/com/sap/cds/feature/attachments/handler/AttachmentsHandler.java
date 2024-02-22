package com.sap.cds.feature.attachments.handler;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.model.ModelConstants;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentsHandler implements EventHandler {

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

		@Before(event = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void uploadAttachments(EventContext context, List<CdsData> data) {

				//check if entity has association to attachments //TODO: recursive
				if (context.getTarget().findAssociation("attachments").isPresent()) { //how to know if this association is of type
						CdsDataProcessor.create().addConverter(
										(path, element, type) -> path.target().type().getAnnotationValue("_is_media_data", false) && element.getName().equals("content"), // filter
										(path, element, value) -> {
												AttachmentStorageResult uploadResult = null;
												try {
//									final String fileName = path.target();
														final String parentId = path.root().keys().get("ID").toString();
														var storageContext = AttachmentStoreEventContext.create();
														// TODO fill context
														uploadResult = attachmentService.storeAttachment(storageContext);
												} catch (AttachmentAccessException e) {
														throw new RuntimeException(e);
												}
												return uploadResult.documentId(); //convert value of content to null in db; attachmentService should return null if it is not the default service (i.e. s3 or sdm)
										})
								.process(data, context.getTarget());
				}
		}

}

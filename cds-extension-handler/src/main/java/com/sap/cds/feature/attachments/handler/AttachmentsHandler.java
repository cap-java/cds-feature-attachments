package com.sap.cds.feature.attachments.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.ApplicationEventFactory;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class AttachmentsHandler implements EventHandler {

		private static final Logger logger = LoggerFactory.getLogger(AttachmentsHandler.class);
		private final ApplicationEventFactory eventProcessor;

		public AttachmentsHandler(ApplicationEventFactory eventProcessor) {
				this.eventProcessor = eventProcessor;
		}

		@After
		@HandlerOrder(HandlerOrder.EARLY)
		void readAttachments(CdsReadEventContext context, List<CdsData> data) {

				//TODO Implement
				//TODO Implement content replacement for read of attachment entity
				//fill mimeType for content in case entity with MediaType annotation is requested

//				var cdsModel = context.getCdsRuntime().getCdsModel();
//				if (data.size() != 1) {  //content is to be read only for single attachment
//						return;
//				}
//
//				if (context.getTarget().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)) {
//
//						final CqnSelect cqn = context.getCqn();
//						// read from attachment service only if content is asked
//						if (cqn.items().stream().filter(i -> i.isRef()).map(i -> i.asRef())
//								.anyMatch(i -> i.path().equals("content"))) { // for delete this condition is not fulfilled
//
//								var attachmentId = CqnAnalyzer.create(cdsModel).analyze(cqn).targetKeys().get("ID").toString();
//								var readContext = AttachmentReadEventContext.create();
//								readContext.setDocumentId(attachmentId);
//								var content = attachmentService.readAttachment(readContext);
//								var existingContent = data.get(0).get("content");
//								if (Objects.isNull(existingContent)) {
//										data.get(0).put("content", content);
//								}
//						}
//				}
		}

		@After(event = {CqnService.EVENT_DELETE})
		void deleteAttachments(CdsDeleteEventContext context) {

				//TODO Implement
				//TODO implement cascading delete e.g. Root is deleted and items -> attachments shall also be deleted

//				var cdsModel = context.getCdsRuntime().getCdsModel();
//
//				//check if entity is of type attachment
//				if (context.getTarget().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)) {
//						final String attachmentId = CqnAnalyzer.create(cdsModel).analyze(context.getCqn()).targetKeys().get("ID").toString();
//						var deleteContext = AttachmentDeleteEventContext.create();
//						//TODO fill attachment id
//						attachmentService.deleteAttachment(deleteContext);
//
//				} else if (context.getTarget().findAssociation("attachments").isPresent()) {
//						//check if parent entity has association to attachments
////						final String up_Id = CqnAnalyzer.create(cdsModel).analyze(context.getCqn()).targetKeys().get("ID").toString();
//						var deleteContext = AttachmentDeleteEventContext.create();
////						deleteContext.setAttachmentId(); TODO fill attachment id
//						attachmentService.deleteAttachment(deleteContext);
//				}

		}

		//TODO UPSERT?
		@Before(event = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		void uploadAttachments(EventContext context, List<CdsData> data) {
				//TODO implement cascading delete if association entity is removed
				var event = context.getEvent();
				logger.info("Attachment processing will be called for event {}", event);
				eventProcessor.getApplicationEvent(event).process(context, data);
		}


}

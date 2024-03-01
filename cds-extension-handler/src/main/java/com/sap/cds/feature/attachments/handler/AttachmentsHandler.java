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

		@Before
		@HandlerOrder(HandlerOrder.EARLY)
		public void readAttachmentsBeforeEvent(CdsReadEventContext context) {
				var event = context.getEvent();
				logger.info("Attachment processing will be called for @Before for event {}", event);
				eventProcessor.getApplicationEvent(event).processBefore(context);
		}

		@After
		@HandlerOrder(HandlerOrder.EARLY)
		public void readAttachmentsAfterEvent(CdsReadEventContext context, List<CdsData> data) {
				var event = context.getEvent();
				logger.info("Attachment processing will be called for @After for event {}", event);
				eventProcessor.getApplicationEvent(event).processAfter(context, data);
		}

		@Before(event = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE})
		@HandlerOrder(HandlerOrder.EARLY)
		public void uploadAttachments(EventContext context, List<CdsData> data) {
				//TODO implement cascading delete if association entity is removed
				var event = context.getEvent();
				logger.info("Attachment processing will be called for event {}", event);
				eventProcessor.getApplicationEvent(event).processAfter(context, data);
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

}

package com.sap.cds.feature.attachments.handler.applicationservice;

import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class DeleteAttachmentsHandler implements EventHandler {

	private final AttachmentsReader attachmentsReader;

	public DeleteAttachmentsHandler(AttachmentsReader attachmentsReader) {
		this.attachmentsReader = attachmentsReader;
	}

	@Before(event = CqnService.EVENT_DELETE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsDeleteEventContext context) {
		var attachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget(), context.getCqn());
	}


	@After(event = CqnService.EVENT_DELETE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processAfter(CdsDeleteEventContext context) {

		//TODO Implement
		//TODO implement cascading delete e.g. Root is deleted and items -> attachments shall also be deleted

//				var cdsModel = context.getModel();
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

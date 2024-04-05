package com.sap.cds.feature.attachments.handler.applicationservice;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
	* The class {@link DeleteAttachmentsHandler} is an event handler that is
	* responsible for deleting attachments for entities.
	* It is called before a delete event is executed.
	*/
@ServiceName(value = "*", type = ApplicationService.class)
public class DeleteAttachmentsHandler implements EventHandler {

	private final AttachmentsReader attachmentsReader;
	private final ModifyAttachmentEvent deleteContentAttachmentEvent;

	public DeleteAttachmentsHandler(AttachmentsReader attachmentsReader, ModifyAttachmentEvent deleteContentAttachmentEvent) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
	}

	@Before(event = CqnService.EVENT_DELETE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsDeleteEventContext context) {
		var attachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget(), context.getCqn());
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> deleteContentAttachmentEvent.processEvent(path, value, CdsData.create(path.target()
																																																																																																																										.values()), context);

		ApplicationHandlerHelper.callProcessor(context.getTarget(), attachments, filter, converter);
	}

}

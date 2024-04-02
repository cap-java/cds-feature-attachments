package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

	private final AttachmentsReader attachmentsReader;
	private final ModifyAttachmentEvent deleteContentAttachmentEvent;

	public DraftCancelAttachmentsHandler(AttachmentsReader attachmentsReader, ModifyAttachmentEvent deleteContentAttachmentEvent) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
	}

	//TODO Unit Tests
	@Before(event = DraftService.EVENT_DRAFT_CANCEL)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(DraftCancelEventContext context, List<CdsData> data) {
		var attachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget()
																																																																												.getTargetOf("SiblingEntity"), context.getCqn());
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> deleteContentAttachmentEvent.processEvent(path, value, CdsData.create(path.target()
																																																																																																																										.values()), context);

		ApplicationHandlerHelper.callProcessor(context.getTarget(), attachments, filter, converter);



	}

}

package com.sap.cds.feature.attachments.handler;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class UpdateAttachmentsHandler implements EventHandler {

	private final PersistenceService persistenceService;
	private final ModifyAttachmentEventFactory eventFactory;

	public UpdateAttachmentsHandler(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
		this.persistenceService = persistenceService;
		this.eventFactory = eventFactory;
	}

	@Before(event = CqnService.EVENT_UPDATE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsUpdateEventContext context, List<CdsData> data) {
		if (!ApplicationHandlerBase.isContentFieldInData(context.getTarget(), data)) {
			return;
		}

		ModifyApplicationHandlerBase.uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_UPDATE, eventFactory, persistenceService);
	}

}

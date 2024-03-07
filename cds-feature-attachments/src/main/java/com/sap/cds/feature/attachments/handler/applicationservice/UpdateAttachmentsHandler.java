package com.sap.cds.feature.attachments.handler.applicationservice;

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
public class UpdateAttachmentsHandler extends ModifyApplicationHandlerBase implements EventHandler {

	public UpdateAttachmentsHandler(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
		super(persistenceService, eventFactory);
	}

	@Before(event = CqnService.EVENT_UPDATE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsUpdateEventContext context, List<CdsData> data) {
		if (processingNotNeeded(context.getTarget(), data)) {
			return;
		}

		uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_UPDATE);
	}

}

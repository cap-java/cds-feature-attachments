package com.sap.cds.feature.attachments.handler.draft;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftHandler implements EventHandler {

	@Before(event = {DraftService.EVENT_DRAFT_SAVE, DraftService.EVENT_DRAFT_PATCH, DraftService.EVENT_DRAFT_PREPARE,
			DraftService.EVENT_DRAFT_EDIT, DraftService.EVENT_ACTIVE_READ, DraftService.EVENT_DRAFT_CREATE,
			DraftService.EVENT_DRAFT_NEW, DraftService.EVENT_DRAFT_READ})
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(EventContext context, List<CdsData> data) {
	}

	@After(event = {DraftService.EVENT_DRAFT_SAVE, DraftService.EVENT_DRAFT_PATCH, DraftService.EVENT_DRAFT_PREPARE,
			DraftService.EVENT_DRAFT_EDIT, DraftService.EVENT_ACTIVE_READ, DraftService.EVENT_DRAFT_CREATE,
			DraftService.EVENT_DRAFT_NEW, DraftService.EVENT_DRAFT_READ})
	@HandlerOrder(HandlerOrder.LATE)
	public void processAfter(EventContext context, List<CdsData> data) {
	}

}

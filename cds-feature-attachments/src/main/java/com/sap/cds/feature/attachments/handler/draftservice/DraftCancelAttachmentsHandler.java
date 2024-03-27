package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

	//TODO Unit Tests
	@Before(event = DraftService.EVENT_DRAFT_CANCEL)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftCancel(EventContext context, List<CdsData> data) {

	}

}

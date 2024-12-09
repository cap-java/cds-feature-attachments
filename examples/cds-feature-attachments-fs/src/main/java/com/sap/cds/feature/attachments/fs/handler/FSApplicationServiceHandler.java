package com.sap.cds.feature.attachments.fs.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.draft.DraftCreateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.utils.OrderConstants;

@ServiceName(value = "*", type = ApplicationService.class)
public class FSApplicationServiceHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(FSApplicationServiceHandler.class);

	@Before
	public void processBeforeNewDraft(DraftCreateEventContext context, List<CdsData> data) {
		logger.info("Creating new draft for entity {}", context.getTarget());
	}

	@Before
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES + 1)
	public void processCreateAttachments(CdsCreateEventContext context, List<CdsData> data) {
		// TODO: add real implementation
	}

	@Before
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES + 1)
	public void processUpdateAttachments(CdsUpdateEventContext context, List<CdsData> data) {
		// TODO: add real implementation
	}

}

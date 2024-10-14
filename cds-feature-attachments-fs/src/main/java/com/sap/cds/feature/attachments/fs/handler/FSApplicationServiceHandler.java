package com.sap.cds.feature.attachments.fs.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.utils.OrderConstants;

@ServiceName(value = "*", type = ApplicationService.class)
public class FSApplicationServiceHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(FSApplicationServiceHandler.class);

	@Before
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES + 1)
	public void processCreateAttachments(CdsCreateEventContext context, List<CdsData> data) {
		// TODO: set size
	}

	@Before
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES + 1)
	public void processUpdateAttachments(CdsUpdateEventContext context, List<CdsData> data) {
		// TODO: set size
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE - 1)
	public void processBeforeDraftPatch(DraftPatchEventContext context, List<CdsData> data) {
		setSize(context, data);
	}

	private void setSize(EventContext context, List<CdsData> data) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();

		Validator validator = (path, element, value) -> {
			if (value instanceof InputStream input) {
				long size = -1; // size is unknown
				try {
					size = input.available();
				} catch (IOException e) {
					logger.error("Error getting size of input stream", e);
				}
				path.target().values().put("size", size);
			}
		};

		CdsDataProcessor.create().addValidator(filter, validator).process(data, context.getTarget());
	}
}

package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftAttachmentsHandler implements EventHandler {

	@Before(event = {DraftService.EVENT_DRAFT_PATCH})
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftPatch(EventContext context, List<CdsData> data) {
		removeDocumentIdForAttachments(context.getTarget(), data);
	}

	void removeDocumentIdForAttachments(CdsEntity entity, List<CdsData> data) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
			path.target().values().put(Attachments.DOCUMENT_ID, null);
			return value;
		};

		ApplicationHandlerHelper.callProcessor(entity, data, filter, converter);
	}

}

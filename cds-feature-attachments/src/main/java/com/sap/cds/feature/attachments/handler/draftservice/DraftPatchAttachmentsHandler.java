package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.Select;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

@ServiceName(value = "*", type = DraftService.class)
public class DraftPatchAttachmentsHandler implements EventHandler {

	//TODO unit tests

	private final PersistenceService persistence;
	private final ModifyAttachmentEventFactory eventFactory;

	public DraftPatchAttachmentsHandler(PersistenceService persistence, ModifyAttachmentEventFactory eventFactory) {
		this.persistence = persistence;
		this.eventFactory = eventFactory;
	}

	@Before(event = DraftService.EVENT_DRAFT_PATCH)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftPatch(EventContext context, List<CdsData> data) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
			var draftElement = path.target().entity().getTargetOf(DraftConstants.SIBLING_ENTITY);
			var select = Select.from(draftElement.getQualifiedName()).matching(path.target().keys());
			var result = persistence.run(select);

			return ModifyApplicationHandlerHelper.handleAttachmentForEntity(result.listOf(CdsData.class), eventFactory, context, path, value);
		};

		ApplicationHandlerHelper.callProcessor(context.getTarget(), data, filter, converter);
	}

}

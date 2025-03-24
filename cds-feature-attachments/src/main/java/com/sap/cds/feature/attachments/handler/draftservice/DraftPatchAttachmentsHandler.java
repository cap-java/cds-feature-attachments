/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.draftservice.constants.DraftConstants;
import com.sap.cds.ql.Select;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

/**
 * The class {@link DraftPatchAttachmentsHandler} is an event handler that is called before a draft patch event is
 * executed. The handler checks the attachments of the draft entity and calls the event factory and corresponding
 * events.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftPatchAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DraftPatchAttachmentsHandler.class);

	private final PersistenceService persistence;
	private final ModifyAttachmentEventFactory eventFactory;

	public DraftPatchAttachmentsHandler(PersistenceService persistence, ModifyAttachmentEventFactory eventFactory) {
		this.persistence = persistence;
		this.eventFactory = eventFactory;
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftPatch(DraftPatchEventContext context, List<CdsData> data) {
		logger.debug("Processing before draft patch event for entity {}", context.getTarget().getName());

		Converter converter = (path, element, value) -> {
			var draftElement = path.target().entity().getQualifiedName().endsWith(DraftConstants.DRAFT_TABLE_POSTFIX)
					? path.target().entity()
					: path.target().entity().getTargetOf(DraftConstants.SIBLING_ENTITY);
			var select = Select.from(draftElement.getQualifiedName()).matching(path.target().keys());
			var result = persistence.run(select);

			return ModifyApplicationHandlerHelper.handleAttachmentForEntity(result.listOf(CdsData.class), eventFactory,
					context, path, (InputStream) value);
		};

		ApplicationHandlerHelper.callProcessor(context.getTarget(), data, ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter);
	}

}

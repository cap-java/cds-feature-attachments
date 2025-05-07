/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.draft.Drafts;
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
			CdsEntity draftEntity = DraftUtils.getDraftEntity(path.target().entity());
			CqnSelect select = Select.from(draftEntity.getQualifiedName()).matching(path.target().keys());
			Result result = persistence.run(select);

			return ModifyApplicationHandlerHelper.handleAttachmentForEntity(result.listOf(CdsData.class), eventFactory,
					context, path, (InputStream) value);
		};

		CdsDataProcessor.create().addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter).process(data,
				context.getTarget());
	}

}

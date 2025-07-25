/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ReadonlyDataContextEnhancer;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.utils.OrderConstants;

/**
 * The class {@link CreateAttachmentsHandler} is an event handler that is responsible for creating attachments for
 * entities. It is called before a create event is executed.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class CreateAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(CreateAttachmentsHandler.class);

	private final ModifyAttachmentEventFactory eventFactory;
	private final ThreadDataStorageReader storageReader;

	public CreateAttachmentsHandler(ModifyAttachmentEventFactory eventFactory, ThreadDataStorageReader storageReader) {
		this.eventFactory = requireNonNull(eventFactory, "eventFactory must not be null");
		this.storageReader = requireNonNull(storageReader, "storageReader must not be null");
	}

	@Before
	@HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
	void processBeforeForDraft(CdsCreateEventContext context, List<CdsData> data) {
		// before the attachment's readonly fields are removed by the runtime, preserve them in a custom field in data
		ReadonlyDataContextEnhancer.preserveReadonlyFields(context.getTarget(), data, storageReader.get());
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	void processBefore(CdsCreateEventContext context, List<CdsData> data) {
		if (ApplicationHandlerHelper.containsContentField(context.getTarget(), data)) {
			logger.debug("Processing before {} event for entity {}", context.getEvent(), context.getTarget());
			ModifyApplicationHandlerHelper.handleAttachmentForEntities(context.getTarget(), data, new ArrayList<>(),
					eventFactory, context);
		}
	}
}

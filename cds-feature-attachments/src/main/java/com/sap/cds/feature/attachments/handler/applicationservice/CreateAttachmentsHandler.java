package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.utilities.LoggingMarker;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
	* The class {@link CreateAttachmentsHandler} is an event handler that is
	* responsible for creating attachments for entities.
	* It is called before a create event is executed.
	*/
@ServiceName(value = "*", type = ApplicationService.class)
public class CreateAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(CreateAttachmentsHandler.class);
	private static final Marker marker = LoggingMarker.APPLICATION_CREATE_HANDLER.getMarker();

	private final ModifyAttachmentEventFactory eventFactory;
	private CdsDataProcessor processor = CdsDataProcessor.create();

	public CreateAttachmentsHandler(ModifyAttachmentEventFactory eventFactory) {
		this.eventFactory = eventFactory;
	}

	@Before(event = CqnService.EVENT_CREATE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsCreateEventContext context, List<CdsData> data) {
		if (!ApplicationHandlerHelper.isContentFieldInData(context.getTarget(), data)) {
			return;
		}

		logger.debug(marker, "Processing before create event for entity {}", context.getTarget().getName());
		setKeysInData(context.getTarget(), data);
		ModifyApplicationHandlerHelper.handleAttachmentForEntities(context.getTarget(), data, new ArrayList<>(), eventFactory, context);
	}

	private void setKeysInData(CdsEntity entity, List<CdsData> data) {
		processor.addGenerator(
						(path, element, type) -> element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
						(path, element, isNull) -> UUID.randomUUID().toString())
				.process(data, entity);
	}

}

package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class CreateAttachmentsHandler implements EventHandler {

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

		setKeysInData(context.getTarget(), data);
		ModifyApplicationHandlerHelper.uploadAttachmentForEntity(context.getTarget(), data, new ArrayList<>(), eventFactory, context);
	}

	private void setKeysInData(CdsEntity entity, List<CdsData> data) {
		processor.addGenerator(
						(path, element, type) -> element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
						(path, element, isNull) -> UUID.randomUUID().toString())
				.process(data, entity);
	}

}

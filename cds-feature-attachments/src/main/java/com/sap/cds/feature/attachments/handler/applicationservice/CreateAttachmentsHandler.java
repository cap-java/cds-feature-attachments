package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.List;
import java.util.UUID;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class CreateAttachmentsHandler implements EventHandler {

	private final PersistenceService persistenceService;
	private final ModifyAttachmentEventFactory eventFactory;
	private CdsDataProcessor processor = CdsDataProcessor.create();

	public CreateAttachmentsHandler(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
		this.persistenceService = persistenceService;
		this.eventFactory = eventFactory;
	}

	@Before(event = CqnService.EVENT_CREATE)
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsCreateEventContext context, List<CdsData> data) {
		if (!ApplicationHandlerHelper.isContentFieldInData(context.getTarget(), data)) {
			return;
		}

		setKeysInData(context.getTarget(), data);
		ModifyApplicationHandlerHelper.uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_CREATE, eventFactory, persistenceService);
	}

	private void setKeysInData(CdsEntity entity, List<CdsData> data) {
		processor.addGenerator(
						(path, element, type) -> element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
						(path, element, isNull) -> UUID.randomUUID().toString())
				.process(data, entity);
	}

}

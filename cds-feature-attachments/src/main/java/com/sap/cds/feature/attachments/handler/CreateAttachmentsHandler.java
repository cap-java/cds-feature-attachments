package com.sap.cds.feature.attachments.handler;

import java.util.List;
import java.util.UUID;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class CreateAttachmentsHandler extends ModifyApplicationHandlerBase implements EventHandler {

	public CreateAttachmentsHandler(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
		super(persistenceService, eventFactory);
	}

	@After(event = CqnService.EVENT_CREATE)
	@HandlerOrder(HandlerOrder.EARLY)
	public void processAfter(CdsCreateEventContext context, List<CdsData> data) {
		if (processingNotNeeded(context.getTarget(), data)) {
			return;
		}

		setKeysInData(context.getTarget(), data);
		uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_CREATE);
	}

	private void setKeysInData(CdsEntity entity, List<CdsData> data) {
		CdsDataProcessor.create().addGenerator(
						(path, element, type) -> isDefinedKey(path, element) && element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
						(path, element, isNull) -> UUID.randomUUID().toString())
				.process(data, entity);
	}

	private boolean isDefinedKey(Path path, CdsElement element) {
		var keyField = getIdField(path.target());
		return element.getName().equals(keyField);
	}

}
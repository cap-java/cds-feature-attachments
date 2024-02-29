package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

public class CreateApplicationEvent extends ModifyApplicationEventBase implements ApplicationEvent {

		public CreateApplicationEvent(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
				super(persistenceService, eventFactory);
		}

		@Override
		public void process(EventContext context, List<CdsData> data) {
				if (processingNotNeeded(context.getTarget(), data)) {
						return;
				}

				setKeysInData(context.getTarget(), data, new ArrayList<>());
				uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_CREATE, new ArrayList<>());
		}

		private void setKeysInData(CdsEntity entity, List<CdsData> data, List<String> processEntityNames) {
				CdsDataProcessor.create().addGenerator(
								(path, element, type) -> path.target().type().keyElements().count() == 1 && element.isKey() && element.getType().isSimpleType(CdsBaseType.UUID),
								(path, element, isNull) -> UUID.randomUUID().toString())
						.process(data, entity);

				processEntityNames.add(entity.getName());
				entity.associations().forEach(element -> {
						var target = element.getType().as(CdsAssociationType.class).getTarget();
						if (!processEntityNames.contains(target.getName())) {
								setKeysInData(target, data, processEntityNames);
						}
				});
		}

}

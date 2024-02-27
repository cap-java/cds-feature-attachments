package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

public class UpdateApplicationEvent extends ModifyApplicationEventBase implements ApplicationEvent {

		public UpdateApplicationEvent(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
				super(persistenceService, eventFactory);
		}

		@Override
		public void process(EventContext context, List<CdsData> data) {
				uploadAttachmentForEntity(context.getTarget(), data, CqnService.EVENT_UPDATE);
		}

}

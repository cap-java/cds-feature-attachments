package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

abstract class ModifyApplicationEventBase extends ApplicationEventBase {

		private static final Logger logger = LoggerFactory.getLogger(ModifyApplicationEventBase.class);

		private final PersistenceService persistenceService;
		private final ModifyAttachmentEventFactory eventFactory;

		ModifyApplicationEventBase(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
				this.persistenceService = persistenceService;
				this.eventFactory = eventFactory;
		}

		boolean processingNotNeeded(CdsEntity entity, List<CdsData> data) {
				return !isContentFieldInData(entity, data);
		}

		void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event) {
				Filter filter = buildFilterForMediaTypeEntity();
				Converter converter = (path, element, value) -> {
						var fieldNames = getFieldNames(element, path.target());
						var attachmentIdObject = path.target().keys().get(fieldNames.keyField());
						var attachmentId = Objects.nonNull(attachmentIdObject) ? String.valueOf(attachmentIdObject) : null;
						var oldData = CqnService.EVENT_UPDATE.equals(event) ? readExistingData(attachmentId, path.target().entity()) : CdsData.create();

						var eventToProcess = eventFactory.getEvent(event, value, fieldNames, oldData);
						return eventToProcess.processEvent(path, element, fieldNames, value, oldData, attachmentId);
				};
				callProcessor(entity, data, filter, converter);
		}

		private CdsData readExistingData(String attachmentId, CdsEntity entity) {
				if (Objects.isNull(attachmentId)) {
						logger.error("no id provided for attachment entity");
						throw new IllegalStateException("no attachment id provided");
				}

				CqnSelect select = Select.from(entity).byId(attachmentId);
				var result = persistenceService.run(select);
				return result.single();
		}

}

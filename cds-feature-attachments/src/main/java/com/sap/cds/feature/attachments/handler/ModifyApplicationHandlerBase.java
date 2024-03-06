package com.sap.cds.feature.attachments.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.ql.Select;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

abstract class ModifyApplicationHandlerBase extends ApplicationHandlerBase {

	private static final Logger logger = LoggerFactory.getLogger(ModifyApplicationHandlerBase.class);
	private static final String DRAFT_ENTITY_ACTIVE_FIELD = "IsActiveEntity";

	private final PersistenceService persistenceService;
	private final ModifyAttachmentEventFactory eventFactory;

	ModifyApplicationHandlerBase(PersistenceService persistenceService, ModifyAttachmentEventFactory eventFactory) {
		this.persistenceService = persistenceService;
		this.eventFactory = eventFactory;
	}

	boolean processingNotNeeded(CdsEntity entity, List<CdsData> data) {
		return !isContentFieldInData(entity, data);
	}

	void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event) {
		Filter filter = buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
			var targetEntity = path.target().entity();
			var keys = removeDraftKeys(path.target().keys());
			var oldData = getExistingData(event, keys, targetEntity);

			var eventToProcess = eventFactory.getEvent(event, value, oldData);
			return eventToProcess.processEvent(path, element, value, oldData, keys);
		};
		callProcessor(entity, data, filter, converter);
	}

	private CdsData getExistingData(String event, Map<String, Object> keys, CdsEntity entity) {
		return CqnService.EVENT_UPDATE.equals(event) ? readExistingData(keys, entity) : CdsData.create();
	}

	private CdsData readExistingData(Map<String, Object> keys, CdsEntity entity) {
		if (keys.isEmpty()) {
			return CdsData.create();
		}

		var select = Select.from(entity).matching(keys);
		logger.info("Select for reading before data: {}", select);
		var result = persistenceService.run(select);
		logger.info("result from reading before data {}", result);
		if (result.rowCount() > 1) {
			throw new ServiceException("too many results");
		}
		return result.rowCount() == 1 ? result.single() : CdsData.create();
	}

	private Map<String, Object> removeDraftKeys(Map<String, Object> keys) {
		var keyMap = new HashMap<>(keys);
		keyMap.entrySet().removeIf(entry -> isDraftActiveEntityField(entry.getKey()));
		return keyMap;
	}

	private boolean isDraftActiveEntityField(String key) {
		return key.equals(DRAFT_ENTITY_ACTIVE_FIELD);
	}

}

package com.sap.cds.feature.attachments.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.ql.Select;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

public final class ModifyApplicationHandlerBase {

	private static final String DRAFT_ENTITY_ACTIVE_FIELD = "IsActiveEntity";

	private ModifyApplicationHandlerBase() {
	}

	public static void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event, ModifyAttachmentEventFactory eventFactory, PersistenceService persistenceService) {
		Filter filter = ApplicationHandlerBase.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
			var targetEntity = path.target().entity();
			var keys = draftKeysRemoved(path.target().keys());
			var oldData = getExistingData(event, keys, targetEntity, persistenceService);

			var eventToProcess = eventFactory.getEvent(event, value, oldData);
			return eventToProcess.processEvent(path, element, value, oldData, keys);
		};
		ApplicationHandlerBase.callProcessor(entity, data, filter, converter);
	}

	private static CdsData getExistingData(String event, Map<String, Object> keys, CdsEntity entity, PersistenceService persistenceService) {
		return CqnService.EVENT_UPDATE.equals(event) ? readExistingData(keys, entity, persistenceService) : CdsData.create();
	}

	private static CdsData readExistingData(Map<String, Object> keys, CdsEntity entity, PersistenceService persistenceService) {
		if (keys.isEmpty()) {
			return CdsData.create();
		}

		var select = Select.from(entity).matching(keys);
		var result = persistenceService.run(select);
		return result.rowCount() > 0 ? result.single() : CdsData.create();
	}

	private static Map<String, Object> draftKeysRemoved(Map<String, Object> keys) {
		var keyMap = new HashMap<>(keys);
		keyMap.entrySet().removeIf(entry -> isDraftActiveEntityField(entry.getKey()));
		return keyMap;
	}

	private static boolean isDraftActiveEntityField(String key) {
		return key.equals(DRAFT_ENTITY_ACTIVE_FIELD);
	}

}

package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.Select;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

public final class ModifyApplicationHandlerHelper {

	private static final String DRAFT_ENTITY_ACTIVE_FIELD = "IsActiveEntity";

	private ModifyApplicationHandlerHelper() {
	}

	public static void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, String event, ModifyAttachmentEventFactory eventFactory, PersistenceService persistenceService) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
			var targetEntity = path.target().entity();
			var keys = removeDraftKeys(path.target().keys());
			var oldData = getExistingData(event, keys, targetEntity, persistenceService);
			var documentIdExists = path.target().values().containsKey(Attachments.DOCUMENT_ID);
			var documentId = (String) path.target().values().get(Attachments.DOCUMENT_ID);

			var eventToProcess = eventFactory.getEvent(value, documentId, documentIdExists, oldData);
			return eventToProcess.processEvent(path, element, value, oldData, keys);
		};
		ApplicationHandlerHelper.callProcessor(entity, data, filter, converter);
	}

	private static CdsData getExistingData(String event, Map<String, Object> keys, CdsEntity entity, PersistenceService persistenceService) {
		return CqnService.EVENT_UPDATE.equals(event) ? readExistingData(keys, entity, persistenceService) : CdsData.create();
	}

	private static CdsData readExistingData(Map<String, Object> keys, CdsEntity entity, PersistenceService persistenceService) {
		var select = Select.from(entity).matching(keys);
		var result = persistenceService.run(select);
		return result.rowCount() > 0 ? result.single() : CdsData.create();
	}

	private static Map<String, Object> removeDraftKeys(Map<String, Object> keys) {
		var keyMap = new HashMap<>(keys);
		keyMap.entrySet().removeIf(entry -> isDraftActiveEntityField(entry.getKey()));
		return keyMap;
	}

	private static boolean isDraftActiveEntityField(String key) {
		return key.equals(DRAFT_ENTITY_ACTIVE_FIELD);
	}

}

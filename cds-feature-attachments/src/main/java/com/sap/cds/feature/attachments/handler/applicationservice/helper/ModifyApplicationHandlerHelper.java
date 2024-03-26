package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.List;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

public final class ModifyApplicationHandlerHelper {

	private ModifyApplicationHandlerHelper() {
	}

	public static void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data, List<CdsData> existingDataList, ModifyAttachmentEventFactory eventFactory, EventContext eventContext) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
			var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
			var oldData = getExistingData(keys, existingDataList);
			var documentIdExists = path.target().values().containsKey(Attachments.DOCUMENT_ID);
			var documentId = (String) path.target().values().get(Attachments.DOCUMENT_ID);

			var eventToProcess = eventFactory.getEvent(value, documentId, documentIdExists, oldData);
			return eventToProcess.processEvent(path, element, value, oldData, keys, eventContext);
		};
		ApplicationHandlerHelper.callProcessor(entity, data, filter, converter);
	}

	private static CdsData getExistingData(Map<String, Object> keys, List<CdsData> existingDataList) {
		return
				existingDataList.stream()
						.filter(existingData -> ApplicationHandlerHelper.isKeyInData(keys, existingData))
						.findAny()
						.orElse(CdsData.create());
	}

}

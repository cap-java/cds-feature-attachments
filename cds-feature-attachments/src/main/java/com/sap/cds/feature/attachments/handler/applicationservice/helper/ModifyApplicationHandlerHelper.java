/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.List;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

public final class ModifyApplicationHandlerHelper {

	private ModifyApplicationHandlerHelper() {
	}

	public static void handleAttachmentForEntities(CdsEntity entity, List<CdsData> data, List<CdsData> existingDataList,
			ModifyAttachmentEventFactory eventFactory, EventContext eventContext) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> handleAttachmentForEntity(existingDataList, eventFactory,
				eventContext, path, value);
		ApplicationHandlerHelper.callProcessor(entity, data, filter, converter);
	}

	public static Object handleAttachmentForEntity(List<CdsData> existingDataList,
			ModifyAttachmentEventFactory eventFactory, EventContext eventContext, Path path, Object value) {
		var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
		ReadonlyDataContextEnhancer.fillReadonlyInContext((CdsData) path.target().values());
		var existingData = getExistingData(keys, existingDataList);
		var contentIdExists = path.target().values().containsKey(Attachments.CONTENT_ID);
		var contentId = (String) path.target().values().get(Attachments.CONTENT_ID);

		var eventToProcess = eventFactory.getEvent(value, contentId, contentIdExists, existingData);
		return eventToProcess.processEvent(path, value, existingData, eventContext);
	}

	private static CdsData getExistingData(Map<String, Object> keys, List<CdsData> existingDataList) {
		return existingDataList.stream().filter(existingData -> ApplicationHandlerHelper.areKeysInData(keys, existingData))
											.findAny().orElse(CdsData.create());
	}

}

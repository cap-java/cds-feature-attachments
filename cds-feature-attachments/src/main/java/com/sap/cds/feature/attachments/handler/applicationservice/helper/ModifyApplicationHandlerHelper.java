/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

public final class ModifyApplicationHandlerHelper {

	private ModifyApplicationHandlerHelper() {
		// avoid instantiation
	}

	/**
	 * Handles attachments for entities.
	 * 
	 * @param entity           the {@link CdsEntity entity} to handle attachments for
	 * @param data             the given list of {@link CdsData data}
	 * @param existingDataList the given list of existing {@link CdsData data}
	 * @param eventFactory     the {@link ModifyAttachmentEventFactory} to create the corresponding event
	 * @param eventContext     the current {@link EventContext}
	 */
	public static void handleAttachmentForEntities(CdsEntity entity, List<CdsData> data, List<CdsData> existingDataList,
			ModifyAttachmentEventFactory eventFactory, EventContext eventContext) {
		Converter converter = (path, element, value) -> handleAttachmentForEntity(existingDataList, eventFactory,
				eventContext, path, (InputStream) value);

		CdsDataProcessor.create().addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter).process(data, entity);
	}

	public static InputStream handleAttachmentForEntity(List<CdsData> existingDataList,
			ModifyAttachmentEventFactory eventFactory, EventContext eventContext, Path path, InputStream content) {
		var keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
		ReadonlyDataContextEnhancer.fillReadonlyInContext((CdsData) path.target().values());
		var existingData = getExistingData(keys, existingDataList);
		var contentId = (String) path.target().values().get(Attachments.CONTENT_ID);

		// for the current request find the event to process
		ModifyAttachmentEvent eventToProcess = eventFactory.getEvent(content, contentId, existingData);

		// process the event
		return eventToProcess.processEvent(path, content, existingData, eventContext);
	}

	private static CdsData getExistingData(Map<String, Object> keys, List<CdsData> existingDataList) {
		return existingDataList.stream()
				.filter(existingData -> ApplicationHandlerHelper.areKeysInData(keys, existingData)).findAny()
				.orElse(CdsData.create());
	}

}

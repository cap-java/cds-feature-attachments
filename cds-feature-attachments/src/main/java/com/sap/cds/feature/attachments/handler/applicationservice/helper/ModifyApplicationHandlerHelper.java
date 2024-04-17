package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

public final class ModifyApplicationHandlerHelper {

	private ModifyApplicationHandlerHelper() {
	}

	public static void handleAttachmentForEntities(CdsEntity entity, List<CdsData> data, List<CdsData> existingDataList,
			ModifyAttachmentEventFactory eventFactory, EventContext eventContext,
			ReadonlyFieldUpdaterProvider fieldUpdateProvider) {
		Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> handleAttachmentForEntity(existingDataList, eventFactory,
				eventContext, path, value, fieldUpdateProvider);
		ApplicationHandlerHelper.callProcessor(entity, data, filter, converter);
	}

	public static Object handleAttachmentForEntity(List<CdsData> existingDataList,
			ModifyAttachmentEventFactory eventFactory, EventContext eventContext, Path path, Object value,
			ReadonlyFieldUpdaterProvider fieldUpdateProvider) {
		var keys = ApplicationHandlerHelper.removeDraftKeys(path.target().keys());
		var existingData = getExistingData(keys, existingDataList);
		var documentIdExists = path.target().values().containsKey(Attachments.DOCUMENT_ID);
		var documentId = (String) path.target().values().get(Attachments.DOCUMENT_ID);

		var eventToProcess = eventFactory.getEvent(value, documentId, documentIdExists, existingData);
		var resultValue = eventToProcess.processEvent(path, value, existingData, eventContext);
		storeReadonlyFields(path, keys, eventContext, fieldUpdateProvider);
		return resultValue;
	}

	private static CdsData getExistingData(Map<String, Object> keys, List<CdsData> existingDataList) {
		return existingDataList.stream().filter(existingData -> ApplicationHandlerHelper.areKeysInData(keys, existingData))
											.findAny().orElse(CdsData.create());
	}

	private static void storeReadonlyFields(Path path, Map<String, Object> keys, EventContext context,
			ReadonlyFieldUpdaterProvider fieldUpdateProvider) {
		if (Objects.nonNull(fieldUpdateProvider)) {
			var documentId = path.target().values().get(Attachments.DOCUMENT_ID);
			var statusCode = path.target().values().get(Attachments.STATUS_CODE);
			var scannedAt = path.target().values().get(Attachments.SCANNED_AT);
			var cdsData = CdsData.create();
			cdsData.put(Attachments.DOCUMENT_ID, documentId);
			cdsData.put(Attachments.STATUS_CODE, statusCode);
			cdsData.put(Attachments.SCANNED_AT, scannedAt);

			var updater = fieldUpdateProvider.getReadonlyFieldUpdater(path.target().entity(), keys, cdsData);
			context.getChangeSetContext().register(updater);
		}
	}

}

package com.sap.cds.feature.attachments.handler;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.Path;
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
			var attachmentId = getAttachmentId(path, fieldNames);
			var oldData = getExistingData(event, path, attachmentId, fieldNames);

			var eventToProcess = eventFactory.getEvent(event, value, fieldNames, oldData);
			return eventToProcess.processEvent(path, element, fieldNames, value, oldData, attachmentId);
		};
		callProcessor(entity, data, filter, converter);
	}

	private String getAttachmentId(Path path, AttachmentFieldNames fieldNames) {
		var attachmentIdObject = path.target().keys().get(fieldNames.keyField());
		return Objects.nonNull(attachmentIdObject) ? String.valueOf(attachmentIdObject) : null;
	}

	private CdsData getExistingData(String event, Path path, String attachmentId, AttachmentFieldNames fieldNames) {
		return CqnService.EVENT_UPDATE.equals(event) ? readExistingData(attachmentId, fieldNames.keyField(), path.target().entity()) : CdsData.create();
	}

	private CdsData readExistingData(String keyValue, String keyFieldName, CdsEntity entity) {
		if (isKeyEmpty(keyValue, keyFieldName)) {
			logger.error("no id provided for attachment entity");
			throw new IllegalStateException("no attachment id provided");
		}
		var select = Select.from(entity).where(e -> e.get(keyFieldName).eq(keyValue));
		var result = persistenceService.run(select);
		return result.single();
	}

	private boolean isKeyEmpty(String keyValue, String keyFieldName) {
		return Objects.isNull(keyFieldName) || keyFieldName.isEmpty() || Objects.isNull(keyValue) || keyValue.isEmpty();
	}

}

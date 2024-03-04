package com.sap.cds.feature.attachments.handler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.impl.builder.model.Conjunction;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;

abstract class ModifyApplicationHandlerBase extends ApplicationHandlerBase {

	private static final Logger logger = LoggerFactory.getLogger(ModifyApplicationHandlerBase.class);

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
			var fieldNames = getFieldNames(element, path.target());
			var attachmentId = getAttachmentId(path, fieldNames);
			var oldData = getExistingData(event, path);

			var eventToProcess = eventFactory.getEvent(event, value, fieldNames, oldData);
			return eventToProcess.processEvent(path, element, fieldNames, value, oldData, attachmentId);
		};
		callProcessor(entity, data, filter, converter);
	}

	private String getAttachmentId(Path path, AttachmentFieldNames fieldNames) {
		var attachmentIdObject = path.target().keys().get(fieldNames.keyField());
		return Objects.nonNull(attachmentIdObject) ? String.valueOf(attachmentIdObject) : null;
	}

	private CdsData getExistingData(String event, Path path) {
		return CqnService.EVENT_UPDATE.equals(event) ? readExistingData(path.target().keys(), path.target().entity()) : CdsData.create();
	}

	private CdsData readExistingData(Map<String, Object> keys, CdsEntity entity) {
		if (isKeyEmpty(keys)) {
			logger.error("no id provided for attachment entity");
			throw new IllegalStateException("no attachment id provided");
		}

		CqnPredicate whereClause = null;
		for (var entry : keys.entrySet()) {
			if (Objects.nonNull(entry.getValue())) {
				CqnPredicate condition = Select.from(entity).where(e -> e.get(entry.getKey()).eq(entry.getValue())).where().orElseThrow();
				whereClause = Objects.isNull(whereClause) ? condition : Conjunction.and(condition, whereClause);
			}
		}

		var select = Select.from(entity).where(whereClause);
		logger.info("Select for reading before data: {}", select);
		var result = persistenceService.run(select);
		logger.info("result from reading before data {}", result);
		return result.single();
	}

	private boolean isKeyEmpty(Map<String, Object> keys) {
		return keys.values().stream().noneMatch(Objects::nonNull);
	}

}

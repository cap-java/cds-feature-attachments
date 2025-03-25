/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.modifier.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.stream.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
 * The class {@link ReadAttachmentsHandler} is an event handler that is
 * responsible for reading attachments for entities.
 * In the before read event, it modifies the CQN to include the content ID and status.
 * In the after read event, it adds a proxy for the stream of the attachments service to the data.
 * Only if the data are read the proxy forwards the request to the attachment service to read the attachment.
 * This is needed to have a filled stream in the data to enable the OData V4 adapter to enrich the data that
 * a link to the content can be shown on the UI.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ReadAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(ReadAttachmentsHandler.class);

	private final AttachmentService attachmentService;
	private final AttachmentStatusValidator attachmentStatusValidator;
	private final AsyncMalwareScanExecutor asyncMalwareScanExecutor;

	public ReadAttachmentsHandler(AttachmentService attachmentService, 
			AttachmentStatusValidator attachmentStatusValidator, AsyncMalwareScanExecutor asyncMalwareScanExecutor) {
		this.attachmentService = attachmentService;
		this.attachmentStatusValidator = attachmentStatusValidator;
		this.asyncMalwareScanExecutor = asyncMalwareScanExecutor;
	}

	@Before
	@HandlerOrder(HandlerOrder.EARLY)
	public void processBefore(CdsReadEventContext context) {
		logger.debug("Processing before read event for entity {}", context.getTarget().getName());

		var cdsModel = context.getModel();
		var fieldNames = getAttachmentAssociations(cdsModel, context.getTarget(), "", new ArrayList<>());
		if (!fieldNames.isEmpty()) {
			var resultCqn = CQL.copy(context.getCqn(), new BeforeReadItemsModifier(fieldNames));
			context.setCqn(resultCqn);
		}
	}

	@After
	@HandlerOrder(HandlerOrder.EARLY)
	public void processAfter(CdsReadEventContext context, List<CdsData> data) {
		if (ApplicationHandlerHelper.noContentFieldInData(context.getTarget(), data)) {
			return;
		}
		logger.debug("Processing after read event for entity {}", context.getTarget().getName());

		Converter converter = (path, element, value) -> {
			logger.info("Processing after read event for entity {}", element.getName());
			var contentId = (String) path.target().values().get(Attachments.CONTENT_ID);
			var status = (String) path.target().values().get(Attachments.STATUS);
			var content = (InputStream) path.target().values().get(Attachments.CONTENT);
			var contentExists = Objects.nonNull(content);
			if (Objects.nonNull(contentId) || contentExists) {
				verifyStatus(path, status, contentId, contentExists);
				Supplier<InputStream> supplier = Objects.nonNull(content) ? () -> content : () -> attachmentService.readAttachment(
						contentId);
				return new LazyProxyInputStream(supplier, attachmentStatusValidator, status);
			} else {
				return value;
			}
		};

		CdsDataProcessor.create().addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter).process(data,
				context.getTarget());
	}

	private List<String> getAttachmentAssociations(CdsModel model, CdsEntity entity, String associationName,
			List<String> processedEntities) {
		var associationNames = new ArrayList<String>();
		if (ApplicationHandlerHelper.isMediaEntity(entity)) {
			associationNames.add(associationName);
		}

		Map<String, CdsEntity> annotatedEntitiesMap = entity.associations()
				.collect(Collectors.toMap(CdsElementDefinition::getName,
						element -> element.getType().as(CdsAssociationType.class).getTarget()));

		if (annotatedEntitiesMap.isEmpty()) {
			return associationNames;
		}

		for (var associatedElement : annotatedEntitiesMap.entrySet()) {
			if (!associationNames.contains(associatedElement.getKey()) && !processedEntities.contains(
					associatedElement.getKey()) && !Drafts.SIBLING_ENTITY.equals(associatedElement.getKey())) {
				processedEntities.add(associatedElement.getKey());
				var result = getAttachmentAssociations(model, associatedElement.getValue(), associatedElement.getKey(),
						processedEntities);
				associationNames.addAll(result);
			}
		}
		return associationNames;
	}

	private void verifyStatus(Path path, String status, String contentId, boolean contentExists) {
		if (areKeysEmpty(path.target().keys())) {
			logger.info("In verify status for content id {} and status {}", contentId, status);
			if ((StatusCode.UNSCANNED.equals(status) || StatusCode.SCANNING.equals(status)) && contentExists) {
				logger.info("Scanning content with ID {} for malware, has current status {}", contentId, status);
				asyncMalwareScanExecutor.scanAsync(path.target().entity(), contentId);
			}
			attachmentStatusValidator.verifyStatus(status);
		}
	}

	private boolean areKeysEmpty(Map<String, Object> keys) {
		return keys.values().stream().allMatch(Objects::isNull);
	}

}

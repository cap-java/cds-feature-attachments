/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.draftservice.ActiveEntityModifier;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnStructuredTypeRef;
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
import com.sap.cds.services.persistence.PersistenceService;

/**
 * The class {@link ReadAttachmentsHandler} is an event handler that is responsible for reading attachments for
 * entities. In the before read event, it modifies the CQN to include the content ID and status. In the after read
 * event, it adds a proxy for the stream of the attachments service to the data. Only if the data are read the proxy
 * forwards the request to the attachment service to read the attachment. This is needed to have a filled stream in the
 * data to enable the OData V4 adapter to enrich the data that a link to the content can be shown on the UI.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ReadAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(ReadAttachmentsHandler.class);

	private final AttachmentService attachmentService;
	private final AttachmentStatusValidator statusValidator;
	private final AsyncMalwareScanExecutor scanExecutor;
	private final PersistenceService persistenceService;

	public ReadAttachmentsHandler(AttachmentService attachmentService, AttachmentStatusValidator statusValidator,
			AsyncMalwareScanExecutor scanExecutor, PersistenceService persistenceService) {
		this.attachmentService = requireNonNull(attachmentService, "attachmentService must not be null");
		this.statusValidator = requireNonNull(statusValidator, "statusValidator must not be null");
		this.scanExecutor = requireNonNull(scanExecutor, "scanExecutor must not be null");
		this.persistenceService = requireNonNull(persistenceService, "persistenceService must not be null");
	}

	@Before
	@HandlerOrder(HandlerOrder.EARLY)
	void processBefore(CdsReadEventContext context) {
		logger.debug("Processing before read event for entity {}", context.getTarget().getName());

		CdsModel cdsModel = context.getModel();
		List<String> fieldNames = getAttachmentAssociations(cdsModel, context.getTarget(), "", new ArrayList<>());
		if (!fieldNames.isEmpty()) {
			CqnSelect resultCqn = CQL.copy(context.getCqn(), new BeforeReadItemsModifier(fieldNames));
			context.setCqn(resultCqn);
		}
	}

	@After
	@HandlerOrder(HandlerOrder.EARLY)
	void processAfter(CdsReadEventContext context, List<CdsData> data) {
		if (ApplicationHandlerHelper.noContentFieldInData(context.getTarget(), data)) {
			return;
		}

		// Ensure that the keys of the target entity are present in the data.
		ensureKeysInData(context, data);

		Converter converter = (path, element, value) -> {
			Attachments attachment = Attachments.of(path.target().values());
			InputStream content = attachment.getContent();
			boolean contentExists = nonNull(content);

			if (!contentExists) {
				Optional<LazyProxyInputStream> stream = getContentFromActiveEntity(context, path);
				if (stream.isPresent()) {
					return stream.get();
				}
			}

			if (nonNull(attachment.getContentId()) || contentExists) {
				verifyStatus(path, attachment, contentExists);
				Supplier<InputStream> supplier = contentExists ? () -> content
						: () -> attachmentService.readAttachment(attachment.getContentId());
				return new LazyProxyInputStream(supplier, statusValidator, attachment.getStatus());
			} else {
				return value;
			}
		};

		CdsDataProcessor.create().addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter).process(data,
				context.getTarget());
	}

	/**
	 * This method ensures that the keys of the target entity are present in the data. This is necessary because the
	 * CdsDataProcessor operates on the data and the keys are needed to identify the attachment.
	 * 
	 * @param context the {@link CdsReadEventContext context} containing the model and CQN statement
	 * @param data    the list of CdsData items to be processed
	 */
	private static void ensureKeysInData(CdsReadEventContext context, List<CdsData> data) {
		Map<String, Object> targetKeys = CqnAnalyzer.create(context.getModel()).analyze(context.getCqn()).targetKeys();
		for (CdsData item : data) {
			targetKeys.forEach((key, value) -> {
				if (value != null && !item.containsKey(key)) {
					item.put(key, value);
				}
			});
		}
	}

	private Optional<LazyProxyInputStream> getContentFromActiveEntity(CdsReadEventContext context, Path path) {
		// modify existing CqnStructuredTypeRef to filter for active entities
		CqnStructuredTypeRef ref = (CqnStructuredTypeRef) path.toRef();

		// build a CqnSelect to read the content + status from the active entity
		CqnSelect select = CQL.copy(Select.from(ref).columns(Attachments.CONTENT, Attachments.STATUS),
				new ActiveEntityModifier(true, context.getTarget().getQualifiedName()));

		// read content from the active entity
		Attachments activeAttachment = persistenceService.run(select).first(Attachments.class).orElse(null);
		InputStream activeContent = activeAttachment != null ? activeAttachment.getContent() : null;
		if (activeContent != null) {
			return Optional
					.of(new LazyProxyInputStream(() -> activeContent, statusValidator, activeAttachment.getStatus()));
		}
		return Optional.empty();
	}

	private List<String> getAttachmentAssociations(CdsModel model, CdsEntity entity, String associationName,
			List<String> processedEntities) {
		List<String> associationNames = new ArrayList<>();
		if (ApplicationHandlerHelper.isMediaEntity(entity)) {
			associationNames.add(associationName);
		}

		Map<String, CdsEntity> annotatedEntities = entity.associations().collect(Collectors.toMap(
				CdsElementDefinition::getName, element -> element.getType().as(CdsAssociationType.class).getTarget()));

		if (annotatedEntities.isEmpty()) {
			return associationNames;
		}

		for (Entry<String, CdsEntity> associatedElement : annotatedEntities.entrySet()) {
			if (!associationNames.contains(associatedElement.getKey())
					&& !processedEntities.contains(associatedElement.getKey())
					&& !Drafts.SIBLING_ENTITY.equals(associatedElement.getKey())) {
				processedEntities.add(associatedElement.getKey());
				List<String> result = getAttachmentAssociations(model, associatedElement.getValue(),
						associatedElement.getKey(), processedEntities);
				associationNames.addAll(result);
			}
		}
		return associationNames;
	}

	private void verifyStatus(Path path, Attachments attachment, boolean contentExists) {
		if (areKeysEmpty(path.target().keys())) {
			logger.debug("In verify status for content id {} and status {}", attachment.getContentId(),
					attachment.getStatus());
			if ((StatusCode.UNSCANNED.equals(attachment.getStatus())
					|| StatusCode.SCANNING.equals(attachment.getStatus())) && contentExists) {
				logger.debug("Scanning content with ID {} for malware, has current status {}",
						attachment.getContentId(), attachment.getStatus());
				scanExecutor.scanAsync(path.target().entity(), attachment.getContentId());
			}
			statusValidator.verifyStatus(attachment.getStatus());
		}
	}

	private boolean areKeysEmpty(Map<String, Object> keys) {
		return keys.values().stream().allMatch(Objects::isNull);
	}

}

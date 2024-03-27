package com.sap.cds.feature.attachments.handler.applicationservice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Generator;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.model.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.modifier.ItemModifierProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnReference.Segment;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class ReadAttachmentsHandler implements EventHandler {

	private final AttachmentService attachmentService;
	private final ItemModifierProvider provider;

	public ReadAttachmentsHandler(AttachmentService attachmentService, ItemModifierProvider provider) {
		this.attachmentService = attachmentService;
		this.provider = provider;
	}

	@Before(event = CqnService.EVENT_READ)
	@HandlerOrder(HandlerOrder.EARLY)
	public void processBefore(CdsReadEventContext context) {
		var cdsModel = context.getModel();
		var fieldNames = getAttachmentAssociations(cdsModel, context.getTarget(), "", new ArrayList<>());
		//TODO use content field name directly
		if (!fieldNames.isEmpty()) {
			var resultCqn = CQL.copy(context.getCqn(), provider.getBeforeReadDocumentIdEnhancer(fieldNames));
			context.setCqn(resultCqn);
		}
	}

	@After(event = CqnService.EVENT_READ)
	@HandlerOrder(HandlerOrder.EARLY)
	public void processAfter(CdsReadEventContext context, List<CdsData> data) {
		if (ApplicationHandlerHelper.isContentFieldInData(context.getTarget(), data)) {
			Filter filter = ApplicationHandlerHelper.buildFilterForMediaTypeEntity();
			Generator generator = (path, element, isNull) -> {
				if (path.target().values().containsKey(element.getName())) {
					var documentId = (String) path.target().values().get(Attachments.DOCUMENT_ID);
					if (Objects.nonNull(documentId)) {
						return new LazyProxyInputStream(() -> attachmentService.readAttachment(documentId));
					}
				}
				return null;
			};

			CdsDataProcessor.create().addGenerator(filter, generator).process(data, context.getTarget());
		}
	}

	private List<String> getAttachmentAssociations(CdsModel model, CdsEntity entity, String associationName, List<String> processedEntities) {
		var query = entity.query();
		List<String> entityNames = query.map(cqnSelect -> cqnSelect.from().asRef().segments().stream().map(Segment::id).toList()).orElseGet(() -> List.of(entity.getQualifiedName()));
		var associationNames = new ArrayList<String>();

		entityNames.forEach(name -> {
			var baseEntity = model.findEntity(name);
			baseEntity.ifPresent(base -> {
				if (ApplicationHandlerHelper.isMediaEntity(base)) {
					associationNames.add(associationName);
				}
			});
		});

		Map<String, CdsEntity> annotatedEntitiesMap = entity.elements().filter(element -> element.getType().isAssociation()).collect(Collectors.toMap(CdsElementDefinition::getName, element -> element.getType().as(CdsAssociationType.class).getTarget()));

		if (annotatedEntitiesMap.isEmpty()) {
			return associationNames;
		}

		for (var associatedElement : annotatedEntitiesMap.entrySet()) {
			if (!associationNames.contains(associatedElement.getKey()) && !processedEntities.contains(associatedElement.getKey())) {
				processedEntities.add(associatedElement.getKey());
				var result = getAttachmentAssociations(model, associatedElement.getValue(), associatedElement.getKey(), processedEntities);
				associationNames.addAll(result);
			}
		}
		return associationNames;
	}

}

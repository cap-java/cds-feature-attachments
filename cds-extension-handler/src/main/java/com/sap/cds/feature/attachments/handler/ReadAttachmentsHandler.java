package com.sap.cds.feature.attachments.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Generator;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.DocumentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier.ItemModifierProvider;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnReference.Segment;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

//TODO add Java Doc
//TODO exception handling
@ServiceName(value = "*", type = ApplicationService.class)
public class ReadAttachmentsHandler extends ApplicationEventBase implements EventHandler {

		private final AttachmentService attachmentService;
		private final ItemModifierProvider provider;

		public ReadAttachmentsHandler(AttachmentService attachmentService, ItemModifierProvider provider) {
				this.attachmentService = attachmentService;
				this.provider = provider;
		}

		@Before
		@HandlerOrder(HandlerOrder.EARLY)
		public void processBefore(EventContext context) {
				var readContext = (CdsReadEventContext) context;
				var cdsModel = readContext.getCdsRuntime().getCdsModel();
				var fieldNames = getContentFieldName(cdsModel, readContext.getTarget(), "", new ArrayList<>());
				if (!fieldNames.isEmpty()) {
						var resultCqn = CQL.copy(readContext.getCqn(), provider.getBeforeReadDocumentIdEnhancer(fieldNames));
						readContext.setCqn(resultCqn);
				}
		}

		@After
		@HandlerOrder(HandlerOrder.EARLY)
		public void processAfter(EventContext context, List<CdsData> data) {
				if (isContentFieldInData(context.getTarget(), data)) {
						Filter filter = buildFilterForMediaTypeEntity();
						Generator generator = (path, element, isNull) -> {
								if (path.target().values().containsKey(element.getName())) {
										var fieldNames = getFieldNames(element, path.target());
										if (fieldNames.documentIdField().isPresent()) {
												var documentId = (String) path.target().values().get(fieldNames.documentIdField().get());
												if (Objects.nonNull(documentId)) {
														return new LazyProxyInputStream(() -> {
																var readContext = AttachmentReadEventContext.create();
																readContext.setDocumentId(documentId);
																return attachmentService.readAttachment(readContext);
														});
												}
										}
								}
								return null;
						};

						CdsDataProcessor.create().addGenerator(filter, generator).process(data, context.getTarget());
				}
		}

		private Map<String, DocumentFieldNames> getContentFieldName(CdsModel model, CdsEntity entity, String associationName, List<String> processedEntities) {
				var query = entity.query();
				List<String> entityNames = query.map(cqnSelect -> cqnSelect.from().asRef().segments().stream().map(Segment::id).toList()).orElseGet(() -> List.of(entity.getQualifiedName()));
				Map<String, DocumentFieldNames> associationNameMap = new HashMap<>();

				entityNames.forEach(name -> {
						var baseEntity = model.findEntity(name);
						baseEntity.ifPresent(base -> {
								if (isMediaEntity(base)) {
										var contentFieldName = new AtomicReference<String>();
										var documentIdFieldName = new AtomicReference<String>();
										var contentElement = base.elements().filter(elem -> hasElementAnnotation(elem, ModelConstants.ANNOTATION_MEDIA_TYPE)).findAny();
										var resultName = contentElement.map(CdsElementDefinition::getName);
										resultName.ifPresent(contentFieldName::set);
										var documentIdElement = base.elements().filter(elem -> hasElementAnnotation(elem, ModelConstants.ANNOTATION_IS_EXTERNAL_DOCUMENT_ID)).findAny();
										var documentIdName = documentIdElement.map(CdsElementDefinition::getName);
										documentIdName.ifPresent(documentIdFieldName::set);
										if (Objects.nonNull(contentFieldName.get()) && Objects.nonNull(documentIdFieldName.get())) {
												var fieldNames = new DocumentFieldNames(contentFieldName.get(), documentIdFieldName.get());
												associationNameMap.put(associationName, fieldNames);
										}
								}
						});
				});

				Map<String, CdsEntity> annotatedEntitiesMap = new HashMap<>();
				entity.elements().filter(element -> element.getType().isAssociation()).forEach(element -> annotatedEntitiesMap.put(element.getName(), element.getType().as(CdsAssociationType.class).getTarget()));

				if (annotatedEntitiesMap.isEmpty()) {
						return associationNameMap;
				}

				for (var associatedElement : annotatedEntitiesMap.entrySet()) {
						if (!associationNameMap.containsKey(associatedElement.getKey()) && !processedEntities.contains(associatedElement.getKey())) {
								processedEntities.add(associatedElement.getKey());
								var result = getContentFieldName(model, associatedElement.getValue(), associatedElement.getKey(), processedEntities);
								associationNameMap.putAll(result);
						}
				}
				return associationNameMap;
		}

}

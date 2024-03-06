package com.sap.cds.feature.attachments.handler.draft;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftHandler implements EventHandler {

	@Before(event = {DraftService.EVENT_DRAFT_PATCH})
	@HandlerOrder(HandlerOrder.LATE)
	public void processBeforeDraftPatch(EventContext context, List<CdsData> data) {
		uploadAttachmentForEntity(context.getTarget(), data);
	}

	@Before(event = {DraftService.EVENT_DRAFT_SAVE, DraftService.EVENT_DRAFT_PREPARE,
			DraftService.EVENT_DRAFT_EDIT, DraftService.EVENT_ACTIVE_READ, DraftService.EVENT_DRAFT_CREATE,
			DraftService.EVENT_DRAFT_NEW, DraftService.EVENT_DRAFT_READ})
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(EventContext context, List<CdsData> data) {
	}

	@After(event = {DraftService.EVENT_DRAFT_SAVE, DraftService.EVENT_DRAFT_PATCH, DraftService.EVENT_DRAFT_PREPARE,
			DraftService.EVENT_DRAFT_EDIT, DraftService.EVENT_ACTIVE_READ, DraftService.EVENT_DRAFT_CREATE,
			DraftService.EVENT_DRAFT_NEW, DraftService.EVENT_DRAFT_READ})
	@HandlerOrder(HandlerOrder.LATE)
	public void processAfter(EventContext context, List<CdsData> data) {
	}

	void uploadAttachmentForEntity(CdsEntity entity, List<CdsData> data) {
		Filter filter = buildFilterForMediaTypeEntity();
		Converter converter = (path, element, value) -> {
//			var targetEntity = path.target().entity();
			path.target().values().put(Attachments.DOCUMENT_ID, null);
			return value;
		};
		callProcessor(entity, data, filter, converter);
	}

	protected Filter buildFilterForMediaTypeEntity() {
		return (path, element, type) -> isMediaEntity(path.target().type()) && hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
	}

	protected boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

	protected boolean hasElementAnnotation(CdsElement element, String annotation) {
		return element.findAnnotation(annotation).isPresent();
	}

	protected void callProcessor(CdsEntity entity, List<CdsData> data, Filter filter, Converter converter) {
		CdsDataProcessor.create().addConverter(
						filter, converter)
				.process(data, entity);
	}

}

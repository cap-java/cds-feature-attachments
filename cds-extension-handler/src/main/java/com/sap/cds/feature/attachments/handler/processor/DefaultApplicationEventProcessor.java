package com.sap.cds.feature.attachments.handler.processor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;


public class DefaultApplicationEventProcessor extends ProcessingBase implements ApplicationEventProcessor {

		private final AttachmentService attachmentService;
		private final StoreEvent storeEvent;
		private final UpdateEvent updateEvent;
		private final DeleteContentEvent deleteContentEvent;

		public DefaultApplicationEventProcessor(AttachmentService attachmentService) {
				this.attachmentService = attachmentService;
				this.storeEvent = new StoreEvent(attachmentService);
				this.updateEvent = new UpdateEvent(attachmentService);
				this.deleteContentEvent = new DeleteContentEvent(attachmentService);
		}

		@Override
		public boolean isAttachmentEvent(CdsEntity entity, List<CdsData> data) {
				return isContentFieldInData(entity, data);
		}

		@Override
		public AttachmentEvent getEvent(String event, Object value, AttachmentFieldNames fieldNames, CdsData existingData) {
				return switch (event) {
						case CqnService.EVENT_UPDATE, CqnService.EVENT_CREATE -> {
								if (Objects.isNull(value)) {
										yield deleteContentEvent;
								}
								if (doesDocumentIdExistsBefore(fieldNames, existingData)) {
										yield updateEvent;
								} else {
										yield storeEvent;
								}
						}
						default -> throw new IllegalStateException("Unexpected value: " + event);
				};
		}

		private boolean isContentFieldInData(CdsEntity entity, List<CdsData> data) {
				var isIncluded = new AtomicBoolean();

				Filter filter = (path, element, type) -> path.target().type().getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false)
						&& hasElementAnnotation(element, ModelConstants.ANNOTATION_MEDIA_TYPE);
				Converter converter = (path, element, value) -> {
						isIncluded.set(true);
						return value;
				};

				callProcessor(entity, data, filter, converter);

				if (!isIncluded.get()) {
						entity.associations().forEach(element -> {
								var included = isIncluded.get() || isContentFieldInData(element.getType().as(CdsAssociationType.class).getTarget(), data);
								isIncluded.set(included);
						});
				}

				return isIncluded.get();
		}

}

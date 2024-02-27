package com.sap.cds.feature.attachments.handler.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.CqnService;


public class DefaultApplicationEventProcessor extends ProcessingBase implements ApplicationEventProcessor {

		private final Map<String, ApplicationEvent> applicationEvents = new HashMap<>();

		public DefaultApplicationEventProcessor(ApplicationEvent createApplicationEvent, ApplicationEvent updateApplicationEvent) {
				applicationEvents.put(CqnService.EVENT_CREATE, createApplicationEvent);
				applicationEvents.put(CqnService.EVENT_UPDATE, updateApplicationEvent);
		}

		@Override
		public boolean isAttachmentEvent(CdsEntity entity, List<CdsData> data) {
				return isContentFieldInData(entity, data);
		}

		@Override
		public ApplicationEvent getApplicationEvent(String event) {
				if (!applicationEvents.containsKey(event)) {
						throw new IllegalStateException("expected event not found");
				}

				return applicationEvents.get(event);
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
								var included = isContentFieldInData(element.getType().as(CdsAssociationType.class).getTarget(), data);
								isIncluded.set(included);
						});
				}

				return isIncluded.get();
		}

}

package com.sap.cds.feature.attachments.handler.processor;

import java.util.HashMap;
import java.util.Map;

import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.services.cds.CqnService;


public class DefaultApplicationEventFactory implements ApplicationEventFactory {

		private final Map<String, ApplicationEvent> applicationEvents = new HashMap<>();

		public DefaultApplicationEventFactory(ApplicationEvent createApplicationEvent, ApplicationEvent updateApplicationEvent) {
				applicationEvents.put(CqnService.EVENT_CREATE, createApplicationEvent);
				applicationEvents.put(CqnService.EVENT_UPDATE, updateApplicationEvent);
		}

		@Override
		public ApplicationEvent getApplicationEvent(String event) {
				if (!applicationEvents.containsKey(event)) {
						throw new IllegalStateException("expected event not found");
				}

				return applicationEvents.get(event);
		}

}

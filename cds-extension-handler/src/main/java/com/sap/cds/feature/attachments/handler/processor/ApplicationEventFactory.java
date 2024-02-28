package com.sap.cds.feature.attachments.handler.processor;

import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;

public interface ApplicationEventFactory {

		ApplicationEvent getApplicationEvent(String event);

}

package com.sap.cds.feature.attachments.handler.processor;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.ApplicationEvent;
import com.sap.cds.reflect.CdsEntity;

public interface ApplicationEventProcessor {

		boolean isAttachmentEvent(CdsEntity entity, List<CdsData> data);

		ApplicationEvent getApplicationEvent(String event);

}

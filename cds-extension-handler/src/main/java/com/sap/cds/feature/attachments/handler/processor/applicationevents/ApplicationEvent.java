package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.services.EventContext;

public interface ApplicationEvent {

		void process(EventContext context, List<CdsData> data);

}

package com.sap.cds.feature.attachments.handler.processor.applicationevents;

import java.util.List;

import com.sap.cds.CdsData;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;

public interface ApplicationEvent {

		default void processBefore(EventContext context) {
				throw new ServiceException("not implemented");
		}

		void processAfter(EventContext context, List<CdsData> data);

}

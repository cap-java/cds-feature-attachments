/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
 * The interface {@link ModifyAttachmentEvent} provides a method to process an event on the {@link AttachmentService}.
 */
public interface ModifyAttachmentEvent {

	/**
	 * Processes the event on the {@link AttachmentService}.
	 * @param path 
	 * @param value 
	 * @param existingData
	 * @param eventContext
	 * @return
	 */
	Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext);

}

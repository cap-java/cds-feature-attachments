/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.io.InputStream;

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
	 * 
	 * @param path         the path of the attachment
	 * @param content      the content of the attachment
	 * @param existingData existing data
	 * @param eventContext the current event context
	 * @return
	 */
	InputStream processEvent(Path path, InputStream content, CdsData existingData, EventContext eventContext);

}

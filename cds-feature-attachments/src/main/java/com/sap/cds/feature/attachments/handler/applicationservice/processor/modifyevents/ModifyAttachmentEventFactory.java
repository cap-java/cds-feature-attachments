/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.io.InputStream;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.service.AttachmentService;

/**
 * The class {@link ModifyAttachmentEventFactory} is a factory
 * that creates the corresponding event for the attachment service {@link AttachmentService}.
 * The class is used to determine the event that should be executed based on the content,
 * the contentId and the existingData.
 */
public interface ModifyAttachmentEventFactory {

	/**
	 * Returns the event that should be executed based on the given parameters.
	 * 
	 * @param content        the optional content as {@link InputStream}
	 * @param contentId      the optional content id
	 * @param existingData   the existing {@link CdsData data}
	 * @return the corresponding {@link ModifyAttachmentEvent} that should be executed
	 */
	ModifyAttachmentEvent getEvent(InputStream content, String contentId, CdsData existingData);

}

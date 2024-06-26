/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.service.AttachmentService;

/**
	* The class {@link ModifyAttachmentEventFactory} is a factory
	* that creates the corresponding event for the attachment service {@link AttachmentService}.
	* The class is used to determine the event that should be executed based on the content,
	* the contentId and the existingData.
	*/
public interface ModifyAttachmentEventFactory {

	ModifyAttachmentEvent getEvent(Object content, String contentId, boolean contentIdExist, CdsData existingData);

}

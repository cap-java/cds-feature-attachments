package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;

public interface ModifyAttachmentEventFactory {

	ModifyAttachmentEvent getEvent(Object content, String documentId, boolean documentIdExist, CdsData existingData);

}

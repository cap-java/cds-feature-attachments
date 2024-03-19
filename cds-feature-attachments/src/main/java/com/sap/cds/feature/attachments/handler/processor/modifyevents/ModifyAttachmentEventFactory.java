package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import com.sap.cds.CdsData;

public interface ModifyAttachmentEventFactory {

	ModifyAttachmentEvent getEvent(String event, Object value, CdsData existingData);

}

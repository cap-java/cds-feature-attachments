package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

public interface ModifyAttachmentEvent {

	Object processEvent(Path path, Object value, CdsData existingData, EventContext eventContext);

}

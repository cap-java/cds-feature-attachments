package com.sap.cds.feature.attachments.handler.processor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public interface AttachmentEvent {

		Object processEvent(Path path, CdsElement element, AttachmentFieldNames fieldNames, Object value, CdsData existingData) throws AttachmentAccessException;

}

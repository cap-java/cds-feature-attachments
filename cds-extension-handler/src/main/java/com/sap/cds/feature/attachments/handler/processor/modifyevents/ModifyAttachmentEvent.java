package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsElement;

public interface ModifyAttachmentEvent {

		Object processEvent(Path path, CdsElement element, AttachmentFieldNames fieldNames, Object value, CdsData existingData, String attachmentId) throws AttachmentAccessException;

}

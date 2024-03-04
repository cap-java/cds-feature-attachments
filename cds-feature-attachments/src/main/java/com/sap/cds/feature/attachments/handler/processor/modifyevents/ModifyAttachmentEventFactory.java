package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import org.springframework.lang.Nullable;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;

public interface ModifyAttachmentEventFactory {

	ModifyAttachmentEvent getEvent(String event, @Nullable Object value, AttachmentFieldNames fieldNames, CdsData existingData);

}

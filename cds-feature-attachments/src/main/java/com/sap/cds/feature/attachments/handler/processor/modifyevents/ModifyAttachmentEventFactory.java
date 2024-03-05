package com.sap.cds.feature.attachments.handler.processor.modifyevents;

import org.springframework.lang.Nullable;

import com.sap.cds.CdsData;

public interface ModifyAttachmentEventFactory {

	ModifyAttachmentEvent getEvent(String event, @Nullable Object value, CdsData existingData);

}

package com.sap.cds.feature.attachments.handler.processor;

import java.util.List;

import org.springframework.lang.Nullable;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.reflect.CdsEntity;

public interface ApplicationEventProcessor {

		boolean isAttachmentEvent(CdsEntity entity, List<CdsData> data);

		AttachmentEvent getEvent(String event, @Nullable Object value, AttachmentFieldNames fieldNames, CdsData existingData);

}

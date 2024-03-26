package com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.runtime.CdsRuntime;

public class DefaultListenerProvider implements ListenerProvider {
	@Override
	public ChangeSetListener provideListener(String documentId, CdsRuntime cdsRuntime, AttachmentService outboxedAttachmentService) {
		return new CreationChangeSetListener(documentId, cdsRuntime, outboxedAttachmentService);
	}
}

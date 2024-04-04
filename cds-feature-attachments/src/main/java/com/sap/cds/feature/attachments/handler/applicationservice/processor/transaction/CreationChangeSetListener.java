package com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction;

import java.util.function.Consumer;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.runtime.CdsRuntime;

public class CreationChangeSetListener implements ChangeSetListener {

	private final String documentId;
	private final CdsRuntime cdsRuntime;
	private final AttachmentService outboxedAttachmentService;

	public CreationChangeSetListener(String documentId, CdsRuntime cdsRuntime, AttachmentService outboxedAttachmentService) {
		this.documentId = documentId;
		this.cdsRuntime = cdsRuntime;
		this.outboxedAttachmentService = outboxedAttachmentService;
	}

	@Override
	public void afterClose(boolean completed) {
		if (!completed) {
			cdsRuntime.requestContext()
					.run((Consumer<RequestContext>) requestContext -> outboxedAttachmentService.markAsDeleted(documentId));
		}
	}

}

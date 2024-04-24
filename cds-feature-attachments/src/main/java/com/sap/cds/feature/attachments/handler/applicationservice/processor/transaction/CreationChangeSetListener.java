/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction;

import java.util.function.Consumer;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.runtime.CdsRuntime;

/**
	* The class {@link CreationChangeSetListener} is a listener that is called after
	* the transaction is closed for which the listener was registered.
	* It marks the attachment as deleted by calling	the {@link AttachmentService} for the mark as deleted event.
	*/
public class CreationChangeSetListener implements ChangeSetListener {

	private final String contentId;
	private final CdsRuntime cdsRuntime;
	private final AttachmentService outboxedAttachmentService;

	public CreationChangeSetListener(String contentId, CdsRuntime cdsRuntime,
			AttachmentService outboxedAttachmentService) {
		this.contentId = contentId;
		this.cdsRuntime = cdsRuntime;
		this.outboxedAttachmentService = outboxedAttachmentService;
	}

	@Override
	public void afterClose(boolean completed) {
		if (!completed) {
			cdsRuntime.requestContext().run(
					(Consumer<RequestContext>) requestContext -> outboxedAttachmentService.markAttachmentAsDeleted(contentId));
		}
	}

}

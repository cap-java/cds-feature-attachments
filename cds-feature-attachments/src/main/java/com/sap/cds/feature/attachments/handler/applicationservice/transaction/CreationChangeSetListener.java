/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.transaction;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.runtime.CdsRuntime;

/**
 * The class {@link CreationChangeSetListener} is a listener that is called after the transaction is closed for which
 * the listener was registered. It marks the attachment as deleted by calling the {@link AttachmentService} for the mark
 * as deleted event.
 */
public class CreationChangeSetListener implements ChangeSetListener {

	private final String contentId;
	private final CdsRuntime cdsRuntime;
	private final AttachmentService attachmentService;

	/**
	 * Creates a new instance of {@link CreationChangeSetListener}.
	 *
	 * @param contentId         the content ID of the attachment to be marked as deleted
	 * @param cdsRuntime        the {@link CdsRuntime} to access the request context
	 * @param attachmentService the {@link AttachmentService} to mark the attachment as deleted
	 */
	public CreationChangeSetListener(String contentId, CdsRuntime cdsRuntime, AttachmentService attachmentService) {
		this.contentId = requireNonNull(contentId, "contentId must not be null");
		this.cdsRuntime = requireNonNull(cdsRuntime, "cdsRuntime must not be null");
		this.attachmentService = requireNonNull(attachmentService, "attachmentService must not be null");
	}

	@Override
	public void afterClose(boolean completed) {
		if (!completed) {
			cdsRuntime.requestContext().run((Consumer<RequestContext>) requestContext -> attachmentService
					.markAttachmentAsDeleted(new MarkAsDeletedInput(contentId, requestContext.getUserInfo())));
		}
	}

}

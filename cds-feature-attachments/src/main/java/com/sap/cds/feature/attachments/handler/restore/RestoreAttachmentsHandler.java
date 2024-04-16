package com.sap.cds.feature.attachments.handler.restore;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.restoreattachments.RestoreAttachmentsContext;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.restoreattachments.RestoreAttachments_;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(RestoreAttachments_.CDS_NAME)
public class RestoreAttachmentsHandler implements EventHandler {

	private final AttachmentService attachmentService;

	public RestoreAttachmentsHandler(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@On(event = RestoreAttachmentsContext.CDS_NAME)
	public void restoreAttachments(RestoreAttachmentsContext context) {
		attachmentService.restore(context.getRestoreTimestamp());
		context.setCompleted();
	}

}

/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;

/**
 * The class {@link MarkAsDeletedAttachmentEvent} handles the mark of deletion of an attachment. It calls the
 * {@link AttachmentService} to mark the attachment as deleted.
 */
public class MarkAsDeletedAttachmentEvent implements ModifyAttachmentEvent {

	private static final Logger logger = LoggerFactory.getLogger(MarkAsDeletedAttachmentEvent.class);

	private final AttachmentService attachmentService;

	public MarkAsDeletedAttachmentEvent(AttachmentService attachmentService) {
		this.attachmentService = requireNonNull(attachmentService, "attachmentService must not be null");
	}

	@Override
	public InputStream processEvent(Path path, InputStream content, Attachments existingData,
			EventContext eventContext) {
		String qualifiedName = eventContext.getTarget().getQualifiedName();
		logger.debug("Processing the event for calling attachment service with mark as delete event for entity {}",
				qualifiedName);

		if (ApplicationHandlerHelper.doesContentIdExistsBefore(existingData)
				&& !DraftService.EVENT_DRAFT_PATCH.equals(eventContext.getEvent())) {
			logger.debug("Calling attachment service with mark as delete event for entity {}", qualifiedName);
			attachmentService.markAttachmentAsDeleted(
					new MarkAsDeletedInput(existingData.getContentId(), eventContext.getUserInfo()));
		} else {
			logger.debug(
					"Do NOT call attachment service with mark as delete event for entity {} as no document id found in existing data and event is DRAFT_PATCH event",
					qualifiedName);
		}
		if (Objects.nonNull(path)) {
			var newContentId = path.target().values().get(Attachments.CONTENT_ID);
			if (Objects.nonNull(newContentId) && newContentId.equals(existingData.getContentId())
					|| !path.target().values().containsKey(Attachments.CONTENT_ID)) {
				path.target().values().put(Attachments.CONTENT_ID, null);
				path.target().values().put(Attachments.STATUS, null);
				path.target().values().put(Attachments.SCANNED_AT, null);
			}
		}
		return content;
	}

}

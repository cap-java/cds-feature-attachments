/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;

/**
 * The class {@link UpdateAttachmentEvent} is an implementation of the {@link ModifyAttachmentEvent}. The class is used
 * to update an attachment by calling the mark as deleted and create method of the attachment service
 * {@link AttachmentService}. To call these attachment service events the class calls the delete and create event
 * implementation.
 */
public class UpdateAttachmentEvent implements ModifyAttachmentEvent {

	private static final Logger logger = LoggerFactory.getLogger(UpdateAttachmentEvent.class);

	private final CreateAttachmentEvent createEvent;
	private final MarkAsDeletedAttachmentEvent deleteEvent;

	public UpdateAttachmentEvent(CreateAttachmentEvent createEvent,
			MarkAsDeletedAttachmentEvent deleteAttachmentEvent) {
		this.createEvent = requireNonNull(createEvent, "createEvent must not be null");
		this.deleteEvent = requireNonNull(deleteAttachmentEvent, "deleteAttachmentEvent must not be null");
	}

	@Override
	public InputStream processEvent(Path path, InputStream content, CdsData existingData, EventContext eventContext) {
		logger.debug("Processing UPDATE event by calling attachment service with create and delete event for entity {}",
				path.target().entity().getQualifiedName());

		deleteEvent.processEvent(path, content, existingData, eventContext);
		return createEvent.processEvent(path, content, existingData, eventContext);
	}

}

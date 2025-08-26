/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
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

	public UpdateAttachmentEvent(CreateAttachmentEvent createEvent, MarkAsDeletedAttachmentEvent deleteEvent) {
		this.createEvent = requireNonNull(createEvent, "createEvent must not be null");
		this.deleteEvent = requireNonNull(deleteEvent, "deleteEvent must not be null");
	}

	@Override
	public InputStream processEvent(Path path, InputStream content, Attachments attachment, EventContext eventContext) {
		logger.debug("Processing UPDATE event by calling attachment service with create and delete event for entity {}",
				path.target().entity().getQualifiedName());

		deleteEvent.processEvent(path, content, attachment, eventContext);
		return createEvent.processEvent(path, content, attachment, eventContext);
	}

}

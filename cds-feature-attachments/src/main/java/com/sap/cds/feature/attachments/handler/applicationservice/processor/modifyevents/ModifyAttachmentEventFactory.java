/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;

/**
 * The class {@link ModifyAttachmentEventFactory} is a factory class that creates the corresponding event for the
 * attachment service {@link AttachmentService}. The class is used to determine the event that should be executed based
 * on the content, the contentId and the existingData.<br>
 * The events could be:
 * <ul>
 * <li>create</li>
 * <li>update</li>
 * <li>deleteContent</li>
 * <li>doNothing</li>
 * </ul>
 */
public class ModifyAttachmentEventFactory {

	private final CreateAttachmentEvent createEvent;
	private final UpdateAttachmentEvent updateEvent;
	private final MarkAsDeletedAttachmentEvent deleteContentEvent;
	private final DoNothingAttachmentEvent doNothingEvent;

	public ModifyAttachmentEventFactory(CreateAttachmentEvent createEvent, UpdateAttachmentEvent updateEvent,
			MarkAsDeletedAttachmentEvent deleteContentEvent, DoNothingAttachmentEvent doNothingEvent) {
		this.createEvent = requireNonNull(createEvent, "createEvent must not be null");
		this.updateEvent = requireNonNull(updateEvent, "updateEvent must not be null");
		this.deleteContentEvent = requireNonNull(deleteContentEvent, "deleteContentEvent must not be null");
		this.doNothingEvent = requireNonNull(doNothingEvent, "doNothingEvent must not be null");
	}

	/**
	 * Returns the event that should be executed based on the given parameters.
	 * 
	 * @param content        the optional content as {@link InputStream}
	 * @param contentId      the optional content id
	 * @param existingData   the existing {@link CdsData data}
	 * @return the corresponding {@link ModifyAttachmentEvent} that should be executed
	 */
	public ModifyAttachmentEvent getEvent(InputStream content, String contentId, CdsData existingData) {
		var existingContentId = existingData.get(Attachments.CONTENT_ID);
		var event = contentId != null ? handleExistingContentId(content, contentId, (String) existingContentId)
				: handleNonExistingContentId(content, existingContentId);
		return event.orElse(doNothingEvent);
	}

	private Optional<ModifyAttachmentEvent> handleExistingContentId(InputStream content, String contentId,
			String existingContentId) {
		ModifyAttachmentEvent event = null;
		if (contentId.equals(existingContentId) && Objects.nonNull(content)) {
			event = updateEvent;
		}
		if (Objects.nonNull(existingContentId) && !contentId.equals(existingContentId) && Objects.isNull(content)) {
			event = deleteContentEvent;
		}
		if (Objects.nonNull(existingContentId) && !contentId.equals(existingContentId) && Objects.nonNull(content)) {
			event = updateEvent;
		}

		return Optional.ofNullable(event);
	}

	private Optional<ModifyAttachmentEvent> handleNonExistingContentId(Object content, Object existingContentId) {
		ModifyAttachmentEvent event = null;
		if (Objects.nonNull(existingContentId)) {
			if (Objects.nonNull(content)) {
				event = updateEvent;
			} else {
				event = deleteContentEvent;
			}
		} else {
			if (Objects.nonNull(content)) {
				event = createEvent;
			}
		}
		return Optional.ofNullable(event);
	}

}

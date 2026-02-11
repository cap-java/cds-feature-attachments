/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import java.io.InputStream;
import java.util.Optional;

/**
 * The class {@link ModifyAttachmentEventFactory} is a factory class that creates the corresponding
 * event for the attachment service {@link AttachmentService}. The class is used to determine the
 * event that should be executed based on the content, the contentId and the existingData.<br>
 * The events could be:
 *
 * <ul>
 *   <li>create
 *   <li>update
 *   <li>deleteContent
 *   <li>doNothing
 * </ul>
 */
public class ModifyAttachmentEventFactory {

  private final CreateAttachmentEvent createEvent;
  private final UpdateAttachmentEvent updateEvent;
  private final MarkAsDeletedAttachmentEvent deleteEvent;
  private final DoNothingAttachmentEvent doNothingEvent;

  public ModifyAttachmentEventFactory(
      CreateAttachmentEvent createEvent,
      UpdateAttachmentEvent updateEvent,
      MarkAsDeletedAttachmentEvent deleteEvent,
      DoNothingAttachmentEvent doNothingEvent) {
    this.createEvent = requireNonNull(createEvent, "createEvent must not be null");
    this.updateEvent = requireNonNull(updateEvent, "updateEvent must not be null");
    this.deleteEvent = requireNonNull(deleteEvent, "deleteEvent must not be null");
    this.doNothingEvent = requireNonNull(doNothingEvent, "doNothingEvent must not be null");
  }

  /**
   * Returns the event that should be executed based on the given parameters.
   *
   * @param content the optional content as {@link InputStream}
   * @param contentId the optional content id
   * @param attachment the existing {@link CdsData data}
   * @return the corresponding {@link ModifyAttachmentEvent} that should be executed
   */
  public ModifyAttachmentEvent getEvent(
      InputStream content, String contentId, Attachments attachment) {
    Optional<ModifyAttachmentEvent> event =
        contentId != null
            ? handleExistingContentId(content, contentId, attachment.getContentId())
            : handleNonExistingContentId(content, attachment.getContentId());
    return event.orElse(doNothingEvent);
  }

  private Optional<ModifyAttachmentEvent> handleExistingContentId(
      InputStream content, String contentId, String existingContentId) {
    boolean sameContentId = contentId.equals(existingContentId);
    boolean hasContent = content != null;
    boolean hasExistingContent = existingContentId != null;

    if (sameContentId && hasContent) {
      // Same content ID with new content -> update existing attachment
      return Optional.of(updateEvent);
    } else if (hasExistingContent && !sameContentId) {
      // Different content ID with existing content -> update or delete
      return Optional.of(hasContent ? updateEvent : deleteEvent);
    }
    return Optional.empty();
  }

  private Optional<ModifyAttachmentEvent> handleNonExistingContentId(
      Object content, Object existingContentId) {
    boolean hasContent = content != null;
    boolean hasExistingContent = existingContentId != null;

    if (hasExistingContent) {
      // Existing content -> update or delete based on new content presence
      return Optional.of(hasContent ? updateEvent : deleteEvent);
    } else if (hasContent) {
      // No existing content but new content provided -> create
      return Optional.of(createEvent);
    }
    return Optional.empty();
  }
}

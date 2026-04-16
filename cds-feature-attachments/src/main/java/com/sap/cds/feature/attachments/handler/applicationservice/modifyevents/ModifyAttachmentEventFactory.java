/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger logger = LoggerFactory.getLogger(ModifyAttachmentEventFactory.class);

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

  /**
   * Processes an inline attachment event without requiring a {@link com.sap.cds.ql.cqn.Path}. This
   * method determines the appropriate action (create, update, delete, or nothing) and calls the
   * attachment service directly.
   *
   * @param content the content stream (may be null for delete operations)
   * @param contentId the content id from request data (usually null for inline)
   * @param existingAttachment the existing attachment state from the database
   * @param eventContext the current event context
   * @param entity the target entity
   * @param keys the entity keys
   * @return the result content stream, or null
   */
  public InputStream processInlineEvent(
      InputStream content,
      String contentId,
      Attachments existingAttachment,
      EventContext eventContext,
      CdsEntity entity,
      Map<String, Object> keys) {
    ModifyAttachmentEvent event = getEvent(content, contentId, existingAttachment);

    if (event == doNothingEvent) {
      return content;
    }

    if (event == updateEvent) {
      markAsDeletedIfNeeded(existingAttachment, eventContext, entity);
      return createInlineAttachment(content, existingAttachment, eventContext, entity, keys);
    }

    if (event == createEvent) {
      return createInlineAttachment(content, existingAttachment, eventContext, entity, keys);
    }

    if (event == deleteEvent) {
      markAsDeletedIfNeeded(existingAttachment, eventContext, entity);
      existingAttachment.setContentId(null);
      existingAttachment.setStatus(null);
      existingAttachment.setScannedAt(null);
      return null;
    }

    return content;
  }

  private InputStream createInlineAttachment(
      InputStream content,
      Attachments existingAttachment,
      EventContext eventContext,
      CdsEntity entity,
      Map<String, Object> keys) {
    logger.debug(
        "Calling attachment service with create event for inline attachment on entity {}",
        entity.getQualifiedName());

    String mimeType = (String) existingAttachment.get(Attachments.MIME_TYPE);
    String fileName = (String) existingAttachment.get(Attachments.FILE_NAME);

    CreateAttachmentInput createInput =
        new CreateAttachmentInput(keys, entity, fileName, mimeType, content);
    AttachmentModificationResult result =
        createEvent.getAttachmentService().createAttachment(createInput);

    eventContext
        .getChangeSetContext()
        .register(
            createEvent
                .getListenerProvider()
                .provideListener(result.contentId(), eventContext.getCdsRuntime()));

    existingAttachment.setContentId(result.contentId());
    existingAttachment.setStatus(result.status());
    if (result.scannedAt() != null) {
      existingAttachment.setScannedAt(result.scannedAt());
    }

    return result.isInternalStored() ? content : null;
  }

  private void markAsDeletedIfNeeded(
      Attachments existingAttachment, EventContext eventContext, CdsEntity entity) {
    if (existingAttachment.getContentId() != null
        && !DraftService.EVENT_DRAFT_PATCH.equals(eventContext.getEvent())) {
      logger.debug(
          "Calling attachment service with mark as delete event for inline attachment on entity {}",
          entity.getQualifiedName());
      deleteEvent
          .getAttachmentService()
          .markAttachmentAsDeleted(
              new MarkAsDeletedInput(
                  existingAttachment.getContentId(), eventContext.getUserInfo()));
    }
  }

  private Optional<ModifyAttachmentEvent> handleExistingContentId(
      InputStream content, String contentId, String existingContentId) {
    ModifyAttachmentEvent event = null;
    if (Objects.nonNull(existingContentId) && !contentId.equals(existingContentId)) {
      event = Objects.nonNull(content) ? updateEvent : deleteEvent;
    }
    return Optional.ofNullable(event);
  }

  private Optional<ModifyAttachmentEvent> handleNonExistingContentId(
      Object content, Object existingContentId) {
    ModifyAttachmentEvent event = null;
    if (Objects.nonNull(existingContentId)) {
      if (Objects.nonNull(content)) {
        event = updateEvent;
      } else {
        event = deleteEvent;
      }
    } else {
      if (Objects.nonNull(content)) {
        event = createEvent;
      }
    }
    return Optional.ofNullable(event);
  }
}

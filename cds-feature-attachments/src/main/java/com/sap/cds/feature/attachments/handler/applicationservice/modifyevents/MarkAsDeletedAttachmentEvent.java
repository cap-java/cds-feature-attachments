/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link MarkAsDeletedAttachmentEvent} handles the mark of deletion of an attachment. It
 * calls the {@link AttachmentService} to mark the attachment as deleted.
 */
public class MarkAsDeletedAttachmentEvent implements ModifyAttachmentEvent {

  private static final Logger logger = LoggerFactory.getLogger(MarkAsDeletedAttachmentEvent.class);

  private final AttachmentService attachmentService;

  public MarkAsDeletedAttachmentEvent(AttachmentService attachmentService) {
    this.attachmentService =
        requireNonNull(attachmentService, "attachmentService must not be null");
  }

  @Override
  public InputStream processEvent(
      Path path, InputStream content, Attachments attachment, EventContext eventContext) {
    Optional<String> inlinePrefix =
        Optional.ofNullable((String) attachment.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER));

    String qualifiedName = eventContext.getTarget().getQualifiedName();
    logger.debug(
        "Processing the event for calling attachment service with mark as delete event for entity {}",
        qualifiedName);

    if (nonNull(attachment.getContentId())
        && !DraftService.EVENT_DRAFT_PATCH.equals(eventContext.getEvent())) {
      logger.debug(
          "Calling attachment service with mark as delete event for entity {}", qualifiedName);
      attachmentService.markAttachmentAsDeleted(
          new MarkAsDeletedInput(attachment.getContentId(), eventContext.getUserInfo()));
    } else {
      logger.debug(
          "Do NOT call attachment service with mark as delete event for entity {} as no document id found in existing data and event is DRAFT_PATCH event",
          qualifiedName);
    }
    if (nonNull(path)) {
      String contentIdField =
          ApplicationHandlerHelper.resolveFieldName(Attachments.CONTENT_ID, inlinePrefix);
      String statusField =
          ApplicationHandlerHelper.resolveFieldName(Attachments.STATUS, inlinePrefix);
      String scannedAtField =
          ApplicationHandlerHelper.resolveFieldName(Attachments.SCANNED_AT, inlinePrefix);
      String mimeTypeField =
          ApplicationHandlerHelper.resolveFieldName(MediaData.MIME_TYPE, inlinePrefix);
      String fileNameField =
          ApplicationHandlerHelper.resolveFieldName(MediaData.FILE_NAME, inlinePrefix);

      String newContentId = (String) path.target().values().get(contentIdField);
      boolean replacedBySameContent =
          nonNull(newContentId) && newContentId.equals(attachment.getContentId());
      boolean noNewContentSupplied = !path.target().values().containsKey(contentIdField);
      if (replacedBySameContent || noNewContentSupplied) {
        path.target().values().put(contentIdField, null);
        path.target().values().put(statusField, null);
        path.target().values().put(scannedAtField, null);
        if (inlinePrefix.isPresent()) {
          path.target().values().put(mimeTypeField, null);
          path.target().values().put(fileNameField, null);
        }
      }
    }
    return content;
  }
}

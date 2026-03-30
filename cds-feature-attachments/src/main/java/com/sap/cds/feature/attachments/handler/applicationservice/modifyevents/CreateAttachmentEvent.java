/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link CreateAttachmentEvent} handles the creation of an attachment. It calls the
 * {@link AttachmentService} to create the attachment and registers the transaction listener to be
 * able to revert the creation in case of errors.
 */
public class CreateAttachmentEvent implements ModifyAttachmentEvent {

  private static final Logger logger = LoggerFactory.getLogger(CreateAttachmentEvent.class);

  private final AttachmentService attachmentService;
  private final ListenerProvider listenerProvider;

  public CreateAttachmentEvent(
      AttachmentService attachmentService, ListenerProvider listenerProvider) {
    this.attachmentService =
        requireNonNull(attachmentService, "attachmentService must not be null");
    this.listenerProvider = requireNonNull(listenerProvider, "listenerProvider must not be null");
  }

  @Override
  public InputStream processEvent(
      Path path,
      InputStream content,
      Attachments attachment,
      EventContext eventContext,
      Optional<String> inlinePrefix) {
    logger.debug(
        "Calling attachment service with create event for entity {}",
        path.target().entity().getQualifiedName());
    Map<String, Object> values = path.target().values();
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());

    Optional<String> mimeTypeOptional =
        getFieldValue(MediaData.MIME_TYPE, values, attachment, inlinePrefix);
    Optional<String> fileNameOptional =
        getFieldValue(MediaData.FILE_NAME, values, attachment, inlinePrefix);

    CreateAttachmentInput createEventInput =
        new CreateAttachmentInput(
            keys,
            path.target().entity(),
            fileNameOptional.orElse(null),
            mimeTypeOptional.orElse(null),
            content,
            inlinePrefix);
    AttachmentModificationResult result = attachmentService.createAttachment(createEventInput);
    ChangeSetListener createListener =
        listenerProvider.provideListener(result.contentId(), eventContext.getCdsRuntime());

    eventContext.getChangeSetContext().register(createListener);
    // Set contentId and status using correct field names (prefixed for inline)
    String contentIdField =
        inlinePrefix.map(p -> p + "_" + Attachments.CONTENT_ID).orElse(Attachments.CONTENT_ID);
    String statusField =
        inlinePrefix.map(p -> p + "_" + Attachments.STATUS).orElse(Attachments.STATUS);
    path.target().values().put(contentIdField, result.contentId());
    path.target().values().put(statusField, result.status());
    if (nonNull(result.scannedAt())) {
      String scannedAtField =
          inlinePrefix.map(p -> p + "_" + Attachments.SCANNED_AT).orElse(Attachments.SCANNED_AT);
      path.target().values().put(scannedAtField, result.scannedAt());
    }
    return result.isInternalStored() ? content : null;
  }

  private static Optional<String> getFieldValue(
      String fieldName,
      Map<String, Object> values,
      Attachments attachment,
      Optional<String> inlinePrefix) {
    // Try prefixed field name first (for inline types)
    if (inlinePrefix.isPresent()) {
      Object prefixedValue = values.get(inlinePrefix.get() + "_" + fieldName);
      if (nonNull(prefixedValue)) return Optional.of((String) prefixedValue);
    }
    // Fall back to direct field name
    Object annotationValue = values.get(fieldName);
    Object value = nonNull(annotationValue) ? annotationValue : attachment.get(fieldName);
    return Optional.ofNullable((String) value);
  }
}

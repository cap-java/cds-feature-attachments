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
import com.sap.cds.feature.attachments.handler.common.AttachmentFieldResolver;
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
      AttachmentFieldResolver resolver) {
    logger.debug(
        "Calling attachment service with create event for entity {}",
        path.target().entity().getQualifiedName());
    Map<String, Object> values = path.target().values();
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());

    Optional<String> mimeTypeOptional =
        getFieldValue(MediaData.MIME_TYPE, values, attachment, resolver);
    Optional<String> fileNameOptional =
        getFieldValue(MediaData.FILE_NAME, values, attachment, resolver);

    CreateAttachmentInput createEventInput =
        new CreateAttachmentInput(
            keys,
            path.target().entity(),
            fileNameOptional.orElse(null),
            mimeTypeOptional.orElse(null),
            content,
            resolver);
    AttachmentModificationResult result = attachmentService.createAttachment(createEventInput);
    ChangeSetListener createListener =
        listenerProvider.provideListener(result.contentId(), eventContext.getCdsRuntime());

    eventContext.getChangeSetContext().register(createListener);
    path.target().values().put(resolver.contentId(), result.contentId());
    path.target().values().put(resolver.status(), result.status());
    if (nonNull(result.scannedAt())) {
      path.target().values().put(resolver.scannedAt(), result.scannedAt());
    }
    return result.isInternalStored() ? content : null;
  }

  private static Optional<String> getFieldValue(
      String fieldName,
      Map<String, Object> values,
      Attachments attachment,
      AttachmentFieldResolver resolver) {
    // Try prefixed field name first (for inline types)
    if (resolver.isInline()) {
      Object prefixedValue = values.get(resolver.resolve(fieldName));
      if (nonNull(prefixedValue)) return Optional.of((String) prefixedValue);
    }
    // Fall back to direct field name
    Object annotationValue = values.get(fieldName);
    Object value = nonNull(annotationValue) ? annotationValue : attachment.get(fieldName);
    return Optional.ofNullable((String) value);
  }
}

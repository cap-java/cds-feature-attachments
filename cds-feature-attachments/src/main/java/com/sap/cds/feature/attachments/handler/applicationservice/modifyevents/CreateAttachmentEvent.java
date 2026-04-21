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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link CreateAttachmentEvent} handles the creation of an attachment. It calls the
 * {@link AttachmentService} to create the attachment and registers the transaction listener to be
 * able to revert the creation in case of errors.
 */
public class CreateAttachmentEvent implements ModifyAttachmentEvent {

  private static final Logger logger = LoggerFactory.getLogger(CreateAttachmentEvent.class);
  private static final Pattern RFC5987_FILENAME_PATTERN =
      Pattern.compile("filename\\*=UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PLAIN_FILENAME_PATTERN =
      Pattern.compile("filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

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
      Path path, InputStream content, Attachments attachment, EventContext eventContext) {
    logger.debug(
        "Calling attachment service with create event for entity {}",
        path.target().entity().getQualifiedName());
    Map<String, Object> values = path.target().values();
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
    Optional<String> mimeTypeOptional = getFieldValue(MediaData.MIME_TYPE, values, attachment);
    Optional<String> fileNameOptional = getFieldValue(MediaData.FILE_NAME, values, attachment);

    // Fall back to HTTP headers when values are not set in payload
    if (fileNameOptional.isEmpty()) {
      fileNameOptional = extractFileNameFromHeader(eventContext);
      fileNameOptional.ifPresent(fn -> values.put(MediaData.FILE_NAME, fn));
    }
    if (mimeTypeOptional.isEmpty()
        || "application/octet-stream".equalsIgnoreCase(mimeTypeOptional.get())) {
      Optional<String> headerMimeType = extractMimeTypeFromHeader(eventContext);
      if (headerMimeType.isPresent()) {
        mimeTypeOptional = headerMimeType;
        values.put(MediaData.MIME_TYPE, mimeTypeOptional.get());
      }
    }

    CreateAttachmentInput createEventInput =
        new CreateAttachmentInput(
            keys,
            path.target().entity(),
            fileNameOptional.orElse(null),
            mimeTypeOptional.orElse(null),
            content);
    AttachmentModificationResult result = attachmentService.createAttachment(createEventInput);
    ChangeSetListener createListener =
        listenerProvider.provideListener(result.contentId(), eventContext.getCdsRuntime());

    eventContext.getChangeSetContext().register(createListener);
    path.target().values().put(Attachments.CONTENT_ID, result.contentId());
    path.target().values().put(Attachments.STATUS, result.status());
    if (nonNull(result.scannedAt())) {
      path.target().values().put(Attachments.SCANNED_AT, result.scannedAt());
    }
    return result.isInternalStored() ? content : null;
  }

  private static Optional<String> getFieldValue(
      String fieldName, Map<String, Object> values, Attachments attachment) {
    Object annotationValue = values.get(fieldName);
    Object value = nonNull(annotationValue) ? annotationValue : attachment.get(fieldName);
    return Optional.ofNullable((String) value);
  }

  /**
   * Extracts the filename from the Content-Disposition header or falls back to the slug header.
   * Supports RFC 5987 encoded filenames (filename*=UTF-8''...) and plain filenames.
   */
  private static Optional<String> extractFileNameFromHeader(EventContext eventContext) {
    String header = eventContext.getParameterInfo().getHeader("Content-Disposition");
    if (header != null) {
      // Try RFC 5987 encoded filename first (filename*=UTF-8''...)
      Matcher utf8Matcher = RFC5987_FILENAME_PATTERN.matcher(header);
      if (utf8Matcher.find()) {
        return Optional.of(URLDecoder.decode(utf8Matcher.group(1), StandardCharsets.UTF_8));
      }
      // Fall back to plain filename=
      Matcher plainMatcher = PLAIN_FILENAME_PATTERN.matcher(header);
      if (plainMatcher.find()) {
        return Optional.of(plainMatcher.group(1).trim());
      }
    }
    // Fiori Elements may use the slug header instead
    String slug = eventContext.getParameterInfo().getHeader("slug");
    return Optional.ofNullable(slug);
  }

  /**
   * Extracts the MIME type from the Content-Type header, stripping charset and other parameters.
   * Returns empty if the Content-Type is null, empty, or application/octet-stream.
   */
  private static Optional<String> extractMimeTypeFromHeader(EventContext eventContext) {
    String contentType = eventContext.getParameterInfo().getHeader("Content-Type");
    if (contentType == null) {
      return Optional.empty();
    }
    String mimeType = contentType.split(";")[0].trim();
    if (mimeType.isEmpty() || "application/octet-stream".equalsIgnoreCase(mimeType)) {
      return Optional.empty();
    }
    return Optional.of(mimeType);
  }
}

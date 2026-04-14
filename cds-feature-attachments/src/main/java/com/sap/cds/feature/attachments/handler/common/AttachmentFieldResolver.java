/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import java.util.Optional;

/**
 * Resolves attachment field names for both composition-based and inline attachment types. For
 * inline attachments, field names are prefixed (e.g., "profilePicture_contentId"). For
 * composition-based attachments, field names are used directly (e.g., "contentId").
 */
public record AttachmentFieldResolver(Optional<String> inlinePrefix) {

  /** Resolver for composition-based (non-inline) attachments. */
  public static final AttachmentFieldResolver DIRECT =
      new AttachmentFieldResolver(Optional.empty());

  public static AttachmentFieldResolver of(Optional<String> inlinePrefix) {
    return inlinePrefix.isEmpty() ? DIRECT : new AttachmentFieldResolver(inlinePrefix);
  }

  public boolean isInline() {
    return inlinePrefix.isPresent();
  }

  public String contentId() {
    return resolve(Attachments.CONTENT_ID);
  }

  public String status() {
    return resolve(Attachments.STATUS);
  }

  public String scannedAt() {
    return resolve(Attachments.SCANNED_AT);
  }

  public String content() {
    return resolve(MediaData.CONTENT);
  }

  public String mimeType() {
    return resolve(MediaData.MIME_TYPE);
  }

  public String fileName() {
    return resolve(MediaData.FILE_NAME);
  }

  public String resolve(String fieldName) {
    return inlinePrefix.map(p -> p + "_" + fieldName).orElse(fieldName);
  }
}

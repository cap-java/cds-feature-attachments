/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.configuration;

/**
 * Constants for message keys used in localized error messages. These keys correspond to entries in
 * the package-qualified resource bundle {@code com.sap.cds.feature.attachments.i18n.errors}.
 */
public final class MessageKeys {

  // Core module - file size validation
  /** File size exceeds the configured @Validation.Maximum limit. Arg: {0} = max size string. */
  public static final String FILE_SIZE_EXCEEDED =
      "com.sap.cds.feature.attachments.FILE_SIZE_EXCEEDED";

  /** File size exceeds the limit (fallback without size info). */
  public static final String FILE_SIZE_EXCEEDED_NO_SIZE =
      "com.sap.cds.feature.attachments.FILE_SIZE_EXCEEDED_NO_SIZE";

  /** Invalid Content-Length header. */
  public static final String INVALID_CONTENT_LENGTH =
      "com.sap.cds.feature.attachments.INVALID_CONTENT_LENGTH";

  // OSS module - operational errors
  /** Failed to upload file. Arg: {0} = file name. */
  public static final String UPLOAD_FAILED = "com.sap.cds.feature.attachments.UPLOAD_FAILED";

  /** Failed to delete file. Arg: {0} = document id. */
  public static final String DELETE_FAILED = "com.sap.cds.feature.attachments.DELETE_FAILED";

  /** Failed to read file. Arg: {0} = document id. */
  public static final String READ_FAILED = "com.sap.cds.feature.attachments.READ_FAILED";

  /** Document not found. Arg: {0} = document id. */
  public static final String DOCUMENT_NOT_FOUND =
      "com.sap.cds.feature.attachments.DOCUMENT_NOT_FOUND";

  private MessageKeys() {}
}

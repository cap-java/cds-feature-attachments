/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

/**
 * Exception thrown when an attachment document is not found in the object store.
 */
public class AttachmentNotFoundException extends RuntimeException {
  private static final long serialVersionUID = -1234567890123456789L;

  private final String contentId;

  /**
   * Constructs a new AttachmentNotFoundException with the specified detail message and content ID.
   *
   * @param message the detail message
   * @param contentId the ID of the content that was not found
   */
  public AttachmentNotFoundException(String message, String contentId) {
    super(message);
    this.contentId = contentId;
  }

  /**
   * Constructs a new AttachmentNotFoundException with the specified detail message, content ID, and cause.
   *
   * @param message the detail message
   * @param contentId the ID of the content that was not found
   * @param cause the cause of the exception
   */
  public AttachmentNotFoundException(String message, String contentId, Throwable cause) {
    super(message, cause);
    this.contentId = contentId;
  }

  /**
   * Returns the content ID that was not found.
   *
   * @return the content ID
   */
  public String getContentId() {
    return contentId;
  }
}

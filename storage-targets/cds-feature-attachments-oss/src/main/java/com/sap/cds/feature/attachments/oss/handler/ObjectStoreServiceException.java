/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

public class ObjectStoreServiceException extends RuntimeException {
  private static final long serialVersionUID = -360151881669215771L;

  public ObjectStoreServiceException(String message) {
    super(message);
  }

  public ObjectStoreServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}

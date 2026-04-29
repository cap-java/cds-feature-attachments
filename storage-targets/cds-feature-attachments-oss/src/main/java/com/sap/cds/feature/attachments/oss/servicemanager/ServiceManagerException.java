/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.servicemanager;

public class ServiceManagerException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ServiceManagerException(String message) {
    super(message);
  }

  public ServiceManagerException(String message, Throwable cause) {
    super(message, cause);
  }
}

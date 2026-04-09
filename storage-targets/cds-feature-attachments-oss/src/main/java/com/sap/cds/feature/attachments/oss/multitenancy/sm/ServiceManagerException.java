/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.multitenancy.sm;

/** Exception thrown when a Service Manager operation fails. */
public class ServiceManagerException extends RuntimeException {

  public ServiceManagerException(String message) {
    super(message);
  }

  public ServiceManagerException(String message, Throwable cause) {
    super(message, cause);
  }
}

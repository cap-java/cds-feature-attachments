/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.services.ErrorStatus;

public enum ExtendedErrorStatuses implements ErrorStatus {
  CONTENT_TOO_LARGE(413, "Content Too Large", 413);

  private final int code;
  private final String description;
  private final int httpStatus;

  ExtendedErrorStatuses(int code, String description, int httpStatus) {
    this.code = code;
    this.description = description;
    this.httpStatus = httpStatus;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public int getHttpStatus() {
    return httpStatus;
  }

  /**
   * @param code the code
   * @return the ErrorStatus from this enum, associated with the given code or {@code null}
   */
  public static ErrorStatus getByCode(int code) {
    for (ExtendedErrorStatuses errorStatus : values()) {
      if (Integer.parseInt(errorStatus.getCodeString()) == code) {
        return errorStatus;
      }
    }
    return null;
  }

  @Override
  public String getCodeString() {
    return Integer.toString(code);
  }
}

package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.services.ErrorStatus;
import com.sap.cds.services.ErrorStatuses;

public enum ExtendedErrorStatuses implements ErrorStatus {

    CONTENT_TOO_LARGE(413, "The content size exceeds the maximum allowed limit.", 413);
    
    private final int code;
  private final String description;
  private final int httpStatus;

  private ExtendedErrorStatuses(int code, String description, int httpStatus) {
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
    for (ErrorStatus errorStatus : ErrorStatuses.values()) {
      if (errorStatus.getHttpStatus() == code) {
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

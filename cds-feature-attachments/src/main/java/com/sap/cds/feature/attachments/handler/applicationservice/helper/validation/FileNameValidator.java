/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.validation;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;

public final class FileNameValidator {

  public static String validateAndNormalize(String fileName) {
    if (fileName == null) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename must not be null");
    }
    String trimmedFileName = fileName.trim();

    if (trimmedFileName.isEmpty()) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename must not be blank");
    }

    int lastDotIndex = trimmedFileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == trimmedFileName.length() - 1) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Invalid filename format: " + fileName);
    }
    return trimmedFileName;
  }

  private FileNameValidator() {
    // to prevent instantiation
  }
}

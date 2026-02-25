/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.validation;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;

public final class FileNameValidator {

  /**
   * Validates and normalizes a file name before further processing.
   *
   * <p>
   * This method ensures that filenames are non-null, non-blank, and follow a
   * basic "name.extension" format before being used in further processing such as
   * MIME type resolution.
   *
   * @param fileName the original filename to validate
   * @return the trimmed and validated filename
   * @throws ServiceException if the filename is null, blank, or has an invalid
   *                          format
   */
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

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTypeService {
  private static final Logger logger = LoggerFactory.getLogger(MediaTypeService.class);
  public static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";

  /**
   * Resolves the MIME type of a file based on its filename (specifically its extension).
   *
   * @param fileName the name of the file (including extension)
   * @return the resolved MIME type, or a default MIME type if it cannot be determined
   * @throws ServiceException if the filename is null or blank
   */
  public static String resolveMimeType(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
    }

    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
      return fallbackToDefaultMimeType(fileName);
    }

    FileNameMap fileNameMap = URLConnection.getFileNameMap();
    String actualMimeType = fileNameMap.getContentTypeFor(fileName);

    if (actualMimeType == null) {
      return fallbackToDefaultMimeType(fileName);
    }
    return actualMimeType;
  }

  /**
   * Checks if a given MIME type is allowed based on a collection of acceptable media
   *
   * @param acceptableMediaTypes
   * @param mimeType
   * @return
   */
  public static boolean isMimeTypeAllowed(
      Collection<String> acceptableMediaTypes, String mimeType) {
    if (mimeType == null) {
      return false;
    }

    if (acceptableMediaTypes == null
        || acceptableMediaTypes.isEmpty()
        || acceptableMediaTypes.contains("*/*")) return true;

    String baseMimeType = mimeType.trim().toLowerCase();
    Collection<String> normalizedTypes =
        acceptableMediaTypes.stream().map(type -> type.trim().toLowerCase()).toList();

    return normalizedTypes.stream()
        .anyMatch(
            type -> {
              return type.endsWith("/*")
                  ? baseMimeType.startsWith(type.substring(0, type.length() - 1))
                  : baseMimeType.equals(type);
            });
  }

  private static String fallbackToDefaultMimeType(String fileName) {
    logger.warn(
        "Could not determine mime type for file: {}. Setting mime type to default: {}",
        fileName,
        DEFAULT_MEDIA_TYPE);
    return DEFAULT_MEDIA_TYPE;
  }

  private MediaTypeService() {
    // to prevent instantiation
  }
}

/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.media;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTypeService {
  private static final Logger logger = LoggerFactory.getLogger(MediaTypeService.class);
  public static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";
  // TODO: in a different ticket, consider loading this mapping from a JSON file
  // to cover more file extensions
  public static final Map<String, String> EXT_TO_MEDIA_TYPE =
      Map.ofEntries(
          Map.entry("aac", "audio/aac"),
          Map.entry("abw", "application/x-abiword"),
          Map.entry("arc", "application/octet-stream"),
          Map.entry("avi", "video/x-msvideo"),
          Map.entry("azw", "application/vnd.amazon.ebook"),
          Map.entry("bin", "application/octet-stream"),
          Map.entry("png", "image/png"),
          Map.entry("gif", "image/gif"),
          Map.entry("bmp", "image/bmp"),
          Map.entry("bz", "application/x-bzip"),
          Map.entry("bz2", "application/x-bzip2"),
          Map.entry("csh", "application/x-csh"),
          Map.entry("css", "text/css"),
          Map.entry("csv", "text/csv"),
          Map.entry("doc", "application/msword"),
          Map.entry(
              "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
          Map.entry("odp", "application/vnd.oasis.opendocument.presentation"),
          Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
          Map.entry("odt", "application/vnd.oasis.opendocument.text"),
          Map.entry("epub", "application/epub+zip"),
          Map.entry("gz", "application/gzip"),
          Map.entry("htm", "text/html"),
          Map.entry("html", "text/html"),
          Map.entry("ico", "image/x-icon"),
          Map.entry("ics", "text/calendar"),
          Map.entry("jar", "application/java-archive"),
          Map.entry("jpg", "image/jpeg"),
          Map.entry("jpeg", "image/jpeg"),
          Map.entry("js", "text/javascript"),
          Map.entry("json", "application/json"),
          Map.entry("mid", "audio/midi"),
          Map.entry("midi", "audio/midi"),
          Map.entry("mjs", "text/javascript"),
          Map.entry("mov", "video/quicktime"),
          Map.entry("mp3", "audio/mpeg"),
          Map.entry("mp4", "video/mp4"),
          Map.entry("mpeg", "video/mpeg"),
          Map.entry("mpkg", "application/vnd.apple.installer+xml"),
          Map.entry("otf", "font/otf"),
          Map.entry("pdf", "application/pdf"),
          Map.entry("ppt", "application/vnd.ms-powerpoint"),
          Map.entry(
              "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
          Map.entry("rar", "application/x-rar-compressed"),
          Map.entry("rtf", "application/rtf"),
          Map.entry("svg", "image/svg+xml"),
          Map.entry("tar", "application/x-tar"),
          Map.entry("tif", "image/tiff"),
          Map.entry("tiff", "image/tiff"),
          Map.entry("ttf", "font/ttf"),
          Map.entry("vsd", "application/vnd.visio"),
          Map.entry("wav", "audio/wav"),
          Map.entry("woff", "font/woff"),
          Map.entry("woff2", "font/woff2"),
          Map.entry("xhtml", "application/xhtml+xml"),
          Map.entry("xls", "application/vnd.ms-excel"),
          Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
          Map.entry("xml", "application/xml"),
          Map.entry("zip", "application/zip"),
          Map.entry("txt", "text/plain"),
          Map.entry("webp", "image/webp"));

  public static String resolveMimeType(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Filename is missing");
    }
    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
      return fallbackToDefaultMimeType(fileName);
    }
    String fileExtension = fileName.substring(lastDotIndex + 1).toLowerCase();
    String actualMimeType = EXT_TO_MEDIA_TYPE.get(fileExtension);
    if (actualMimeType == null) {
      return fallbackToDefaultMimeType(fileName);
    }
    return actualMimeType;
  }

  private static String fallbackToDefaultMimeType(String fileName) {
    logger.warn(
        "Could not determine mime type for file: {}. Setting mime type to default: {}",
        fileName,
        DEFAULT_MEDIA_TYPE);
    return DEFAULT_MEDIA_TYPE;
  }

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

  private MediaTypeService() {
    // to prevent instantiation
  }
}

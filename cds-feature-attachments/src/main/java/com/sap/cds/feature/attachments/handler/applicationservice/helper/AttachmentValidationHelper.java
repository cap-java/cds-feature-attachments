/*
* © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
*/
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.services.ServiceException;
import com.sap.cds.services.ErrorStatuses;

public class AttachmentValidationHelper {

    public static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";
    public static final Map<String, String> EXT_TO_MEDIA_TYPE = Map.ofEntries(
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
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
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
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
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
            Map.entry("txt", "application/txt"),
            Map.entry("lst", "application/txt"),
            Map.entry("webp", "image/webp"));

    private static final Logger logger = LoggerFactory.getLogger(AttachmentValidationHelper.class);

    /**
     * Validates the file name and resolves its media type. Ensures that the
     * detected media type is part of the list of acceptable media types.
     *
     * @param fileName             the name of the attachment file
     * @param acceptableMediaTypes list of allowed media types (e.g. "image/*",
     *                             "application/pdf")
     * @return the detected media type
     * @throws ServiceException if the file name is invalid or the media type is not
     *                          allowed
     */
    public static String validateMediaTypeForAttachment(String fileName, List<String> acceptableMediaTypes) {
        validateFileName(fileName);
        String detectedMediaType = resolveMimeType(fileName);
        validateAcceptableMediaType(acceptableMediaTypes, detectedMediaType);
        return detectedMediaType;
    }

    private static void validateFileName(String fileName) {
        String clean = fileName.trim();
        int lastDotIndex = clean.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == clean.length() - 1) {
            throw new ServiceException(
                    ErrorStatuses.UNSUPPORTED_MEDIA_TYPE,
                    "Invalid filename format: " + fileName);
        }
    }

    private static void validateAcceptableMediaType(List<String> acceptableMediaTypes, String actualMimeType) {
        if (!checkMimeTypeMatch(acceptableMediaTypes, actualMimeType)) {
            throw new ServiceException(
                    ErrorStatuses.UNSUPPORTED_MEDIA_TYPE,
                    "The attachment file type '{}' is not allowed. Allowed types are: {}", actualMimeType,
                    String.join(", ", acceptableMediaTypes));
        }
    }

    private static String resolveMimeType(String fileName) {
        String actualMimeType = URLConnection.guessContentTypeFromName(fileName);

        if (actualMimeType == null) {
            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            actualMimeType = EXT_TO_MEDIA_TYPE.get(fileExtension);

            if (actualMimeType == null) {
                logger.warn("Could not determine mime type for file: {}. Setting mime type to default: {}",
                        fileName, DEFAULT_MEDIA_TYPE);
                actualMimeType = DEFAULT_MEDIA_TYPE;
            }
        }
        return actualMimeType;
    }

    private static boolean checkMimeTypeMatch(Collection<String> acceptableMediaTypes, String mimeType) {
        if (acceptableMediaTypes == null || acceptableMediaTypes.isEmpty() || acceptableMediaTypes.contains("*/*"))
            return true;

        String baseMimeType = mimeType.trim().toLowerCase();

        return acceptableMediaTypes.stream().anyMatch(type -> {
            String normalizedType = type.trim().toLowerCase();
            return normalizedType.endsWith("/*")
                    ? baseMimeType.startsWith(normalizedType.substring(0, normalizedType.length() - 2) + "/")
                    : baseMimeType.equals(normalizedType);
        });
    }

}
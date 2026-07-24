/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves attachment metadata (file name, MIME type) from HTTP headers and applies it as a
 * fallback to the request data.
 *
 * <p>For direct media uploads the file name is typically provided via the {@code
 * Content-Disposition} or {@code slug} header and the MIME type via the {@code Content-Type} header
 * rather than in the request payload. This resolver normalizes those header values into the
 * attachment data <em>before</em> acceptable-media-type validation runs, so that validation and
 * storage operate on the same file name / MIME type that is ultimately persisted and served.
 */
public final class HeaderMediaMetadataResolver {

  private static final Pattern RFC5987_FILENAME_PATTERN =
      Pattern.compile("filename\\*=UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PLAIN_FILENAME_PATTERN =
      Pattern.compile("(?<!\\*)filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);

  /**
   * Fills the {@code fileName} and {@code mimeType} of every media content attachment in the given
   * data from the request headers, unless those values are already present in the payload.
   *
   * @param entity the {@link CdsEntity} type of the given data
   * @param data the request data to normalize
   * @param eventContext the current {@link EventContext} carrying the request headers
   */
  public static void applyHeaderFallback(
      CdsEntity entity, List<? extends CdsData> data, EventContext eventContext) {
    if (entity == null
        || data == null
        || data.isEmpty()
        || eventContext.getParameterInfo() == null) {
      return;
    }

    CdsDataProcessor.create()
        .addValidator(
            ApplicationHandlerHelper.MEDIA_CONTENT_FILTER,
            (path, element, value) -> {
              Map<String, Object> values = path.target().values();
              if (values.get(MediaData.FILE_NAME) == null) {
                extractFileNameFromHeader(eventContext)
                    .ifPresent(fn -> values.put(MediaData.FILE_NAME, fn));
              }
              if (values.get(MediaData.MIME_TYPE) == null) {
                extractMimeTypeFromHeader(eventContext)
                    .ifPresent(mt -> values.put(MediaData.MIME_TYPE, mt));
              }
            })
        .process(data, entity);
  }

  /**
   * Extracts the file name from the {@code Content-Disposition} header or falls back to the {@code
   * slug} header. Supports RFC 5987 encoded file names ({@code filename*=UTF-8''...}) and plain
   * file names.
   */
  public static Optional<String> extractFileNameFromHeader(EventContext eventContext) {
    String header = eventContext.getParameterInfo().getHeader("Content-Disposition");
    if (header != null) {
      // Try RFC 5987 encoded filename first (filename*=UTF-8''...)
      Matcher utf8Matcher = RFC5987_FILENAME_PATTERN.matcher(header);
      if (utf8Matcher.find()) {
        return Optional.of(URLDecoder.decode(utf8Matcher.group(1), StandardCharsets.UTF_8));
      }
      // Fall back to plain filename=
      Matcher plainMatcher = PLAIN_FILENAME_PATTERN.matcher(header);
      if (plainMatcher.find()) {
        return Optional.of(plainMatcher.group(1).trim());
      }
    }
    // Fiori Elements may use the slug header instead
    String slug = eventContext.getParameterInfo().getHeader("slug");
    if (slug != null) {
      return Optional.of(URLDecoder.decode(slug, StandardCharsets.UTF_8));
    }
    return Optional.empty();
  }

  /**
   * Extracts the MIME type from the {@code Content-Type} header, stripping charset and other
   * parameters. Returns empty if the {@code Content-Type} is null or empty.
   */
  public static Optional<String> extractMimeTypeFromHeader(EventContext eventContext) {
    String contentType = eventContext.getParameterInfo().getHeader("Content-Type");
    if (contentType == null) {
      return Optional.empty();
    }
    String mimeType = contentType.split(";")[0].trim();
    if (mimeType.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(mimeType);
  }

  private HeaderMediaMetadataResolver() {
    // to prevent instantiation
  }
}

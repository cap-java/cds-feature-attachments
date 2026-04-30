/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.CountingInputStream;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModifyApplicationHandlerHelper {

  /** Default max size when malware scanner binding is present (400 MB). */
  public static final String DEFAULT_SIZE_WITH_SCANNER = "400MB";

  /** Effectively unlimited max size when no malware scanner binding is present. */
  public static final String UNLIMITED_SIZE = String.valueOf(Long.MAX_VALUE);

  /**
   * Handles attachments for entities.
   *
   * @param entity the {@link CdsEntity entity} to handle attachments for
   * @param data the given list of {@link CdsData data}
   * @param existingAttachments the given list of existing {@link CdsData data}
   * @param eventFactory the {@link ModifyAttachmentEventFactory} to create the corresponding event
   * @param eventContext the current {@link EventContext}
   * @param defaultMaxSize the default max size to use when no annotation is present
   */
  public static void handleAttachmentForEntities(
      CdsEntity entity,
      List<CdsData> data,
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      String defaultMaxSize) {
    // Condense existing attachments to get a flat list for matching
    List<Attachments> condensedExistingAttachments =
        ApplicationHandlerHelper.condenseAttachments(existingAttachments, entity);

    Converter converter =
        (path, element, value) -> {
          Optional<String> inlinePrefix =
              ApplicationHandlerHelper.getInlineAttachmentPrefix(
                  path.target().entity(), element.getName());
          return handleAttachmentForEntity(
              condensedExistingAttachments,
              eventFactory,
              eventContext,
              path,
              (InputStream) value,
              defaultMaxSize,
              inlinePrefix);
        };

    CdsDataProcessor.create()
        .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
        .process(data, entity);
  }

  /**
   * Handles attachments for a single entity.
   *
   * @param existingAttachments the list of existing {@link Attachments} to check against
   * @param eventFactory the {@link ModifyAttachmentEventFactory} to create the corresponding event
   * @param eventContext the current {@link EventContext}
   * @param path the {@link Path} of the attachment
   * @param content the content of the attachment
   * @param defaultMaxSize the default max size to use when no annotation is present
   * @param inlinePrefix the inline attachment field prefix, or empty for composition-based
   * @return the processed content as an {@link InputStream}
   */
  public static InputStream handleAttachmentForEntity(
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      Path path,
      InputStream content,
      String defaultMaxSize,
      Optional<String> inlinePrefix) {
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
    ReadonlyDataContextEnhancer.restoreReadonlyFields((CdsData) path.target().values());
    Attachments attachment = getExistingAttachment(keys, existingAttachments, inlinePrefix);

    // For inline attachment fields, extract contentId using the known prefix
    String contentId;
    if (inlinePrefix.isPresent()) {
      contentId =
          (String) path.target().values().get(inlinePrefix.get() + "_" + Attachments.CONTENT_ID);
    } else {
      contentId = (String) path.target().values().get(Attachments.CONTENT_ID);
    }

    String contentLength = eventContext.getParameterInfo().getHeader("Content-Length");
    String maxSizeStr = getValMaxValue(path.target().entity(), defaultMaxSize, inlinePrefix);
    eventContext.put(
        "attachment.MaxSize",
        maxSizeStr); // make max size available in context for error handling later
    ServiceException tooLargeException =
        new ServiceException(
            ExtendedErrorStatuses.CONTENT_TOO_LARGE,
            "File size exceeds the limit of {}.",
            maxSizeStr);

    if (contentLength != null) {
      try {
        if (Long.parseLong(contentLength) > FileSizeUtils.parseFileSizeToBytes(maxSizeStr)) {
          throw tooLargeException;
        }
      } catch (NumberFormatException e) {
        throw new ServiceException(ErrorStatuses.BAD_REQUEST, "Invalid Content-Length header");
      }
    }
    CountingInputStream wrappedContent =
        content != null ? new CountingInputStream(content, maxSizeStr) : null;
    ModifyAttachmentEvent eventToProcess =
        eventFactory.getEvent(wrappedContent, contentId, attachment);
    try {
      // Ensure the attachment carries the inline prefix marker for processEvent implementations
      if (inlinePrefix.isPresent()
          && attachment.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER) == null) {
        attachment.put(ApplicationHandlerHelper.INLINE_PREFIX_MARKER, inlinePrefix.get());
      }
      return eventToProcess.processEvent(path, wrappedContent, attachment, eventContext);
    } catch (Exception e) {
      if (wrappedContent != null && wrappedContent.isLimitExceeded()) {
        throw tooLargeException;
      }
      throw e;
    }
  }

  private static String getValMaxValue(
      CdsEntity entity, String defaultMaxSize, Optional<String> inlinePrefix) {
    return entity
        .findElement("content")
        .or(
            () -> {
              if (inlinePrefix.isPresent()) {
                return entity.findElement(inlinePrefix.get() + "_content");
              }
              List<String> prefixes =
                  ApplicationHandlerHelper.getInlineAttachmentFieldNames(entity);
              for (String prefix : prefixes) {
                var found = entity.findElement(prefix + "_content");
                if (found.isPresent()) return found;
              }
              return Optional.empty();
            })
        .flatMap(e -> e.findAnnotation("Validation.Maximum"))
        .map(CdsAnnotation::getValue)
        .filter(v -> !"true".equals(v.toString()))
        .map(Object::toString)
        .orElse(defaultMaxSize);
  }

  private static Attachments getExistingAttachment(
      Map<String, Object> keys,
      List<Attachments> existingAttachments,
      Optional<String> inlinePrefix) {
    return existingAttachments.stream()
        .filter(
            existingData -> {
              // For inline attachments, match by the prefix marker
              if (inlinePrefix.isPresent()) {
                String existingPrefix =
                    (String) existingData.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER);
                return inlinePrefix.get().equals(existingPrefix);
              }
              // For composition-based attachments, match by keys
              return ApplicationHandlerHelper.areKeysInData(keys, existingData);
            })
        .findAny()
        .orElse(Attachments.create());
  }

  private ModifyApplicationHandlerHelper() {
    // avoid instantiation
  }
}

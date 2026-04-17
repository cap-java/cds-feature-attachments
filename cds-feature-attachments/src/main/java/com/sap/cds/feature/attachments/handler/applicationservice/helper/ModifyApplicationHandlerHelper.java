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
import com.sap.cds.feature.attachments.handler.common.InlineAttachmentHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
        (path, element, value) ->
            handleAttachmentForEntity(
                condensedExistingAttachments,
                eventFactory,
                eventContext,
                path,
                (InputStream) value,
                defaultMaxSize);

    CdsDataProcessor.create()
        .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
        .process(data, entity);

    handleInlineAttachments(
        entity, data, existingAttachments, eventFactory, eventContext, defaultMaxSize);
  }

  private static void handleInlineAttachments(
      CdsEntity entity,
      List<CdsData> data,
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      String defaultMaxSize) {
    List<String> inlinePrefixes = InlineAttachmentHelper.findInlineAttachmentPrefixes(entity);
    if (inlinePrefixes.isEmpty()) {
      return;
    }

    for (CdsData row : data) {
      for (String prefix : inlinePrefixes) {
        String contentField = InlineAttachmentHelper.buildInlineFieldName(prefix, "content");
        if (!row.containsKey(contentField)) {
          continue;
        }

        Attachments existingAttachment =
            findExistingInlineAttachment(entity, row, existingAttachments);

        processInlineAttachmentRow(
            row, prefix, existingAttachment, entity, eventFactory, eventContext, defaultMaxSize);
      }
    }
  }

  /**
   * Processes a single inline attachment field within a data row. This method handles content size
   * validation, delegates to the event factory, and writes back attachment metadata (contentId,
   * status, scannedAt) to the row.
   *
   * @param row the data row containing the inline attachment field
   * @param prefix the inline attachment prefix (e.g. "avatar")
   * @param existingAttachment the existing attachment state from the database
   * @param entity the target entity
   * @param eventFactory the event factory for processing
   * @param eventContext the current event context
   * @param defaultMaxSize the default max size to use when no annotation is present
   */
  public static void processInlineAttachmentRow(
      CdsData row,
      String prefix,
      Attachments existingAttachment,
      CdsEntity entity,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      String defaultMaxSize) {
    String contentField = InlineAttachmentHelper.buildInlineFieldName(prefix, "content");
    Object contentValue = row.get(contentField);
    InputStream content = contentValue instanceof InputStream is ? is : null;
    String contentIdField =
        InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.CONTENT_ID);
    String contentId = (String) row.get(contentIdField);

    String contentLength = eventContext.getParameterInfo().getHeader("Content-Length");
    String maxSizeStr = defaultMaxSize;
    eventContext.put("attachment.MaxSize", maxSizeStr);
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

    Map<String, Object> keys = ApplicationHandlerHelper.extractKeys(row, entity);
    Map<String, Object> cleanKeys = ApplicationHandlerHelper.removeDraftKey(keys);

    try {
      InputStream result =
          eventFactory.processInlineEvent(
              wrappedContent,
              contentId,
              existingAttachment,
              eventContext,
              entity,
              cleanKeys,
              row,
              prefix);
      row.put(contentField, result);

      // Only write back metadata when the event actually modified the attachment.
      // For doNothing (e.g. during draft activation), existingAttachment stays empty
      // and we must not overwrite already-restored values in the row.
      if (existingAttachment.containsKey(Attachments.CONTENT_ID)) {
        row.put(
            InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.CONTENT_ID),
            existingAttachment.getContentId());
        row.put(
            InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.STATUS),
            existingAttachment.getStatus());
        if (existingAttachment.getScannedAt() != null) {
          row.put(
              InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.SCANNED_AT),
              existingAttachment.getScannedAt());
        }
      }
    } catch (Exception e) {
      if (wrappedContent != null && wrappedContent.isLimitExceeded()) {
        throw tooLargeException;
      }
      throw e;
    }
  }

  /**
   * Finds the existing inline attachment for a given row by matching entity keys.
   *
   * @param entity the entity to extract keys from
   * @param row the data row
   * @param existingAttachments the list of existing attachments to search
   * @return the matching attachment, or an empty {@link Attachments} if none found
   */
  public static Attachments findExistingInlineAttachment(
      CdsEntity entity, CdsData row, List<Attachments> existingAttachments) {
    Map<String, Object> keys = ApplicationHandlerHelper.extractKeys(row, entity);
    Map<String, Object> cleanKeys = ApplicationHandlerHelper.removeDraftKey(keys);
    return existingAttachments.stream()
        .filter(existing -> existing.containsKey(Attachments.CONTENT_ID))
        .filter(existing -> ApplicationHandlerHelper.areKeysInData(cleanKeys, existing))
        .findAny()
        .orElse(Attachments.create());
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
   * @return the processed content as an {@link InputStream}
   */
  public static InputStream handleAttachmentForEntity(
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      Path path,
      InputStream content,
      String defaultMaxSize) {
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
    ReadonlyDataContextEnhancer.restoreReadonlyFields((CdsData) path.target().values());
    Attachments attachment = getExistingAttachment(keys, existingAttachments);
    String contentId = (String) path.target().values().get(Attachments.CONTENT_ID);
    String contentLength = eventContext.getParameterInfo().getHeader("Content-Length");
    String maxSizeStr = getValMaxValue(path.target().entity(), defaultMaxSize);
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
      return eventToProcess.processEvent(path, wrappedContent, attachment, eventContext);
    } catch (Exception e) {
      if (wrappedContent != null && wrappedContent.isLimitExceeded()) {
        throw tooLargeException;
      }
      throw e;
    }
  }

  private static String getValMaxValue(CdsEntity entity, String defaultMaxSize) {
    return entity
        .findElement("content")
        .flatMap(e -> e.findAnnotation("Validation.Maximum"))
        .map(CdsAnnotation::getValue)
        .filter(v -> !"true".equals(v.toString()))
        .map(Object::toString)
        .orElse(defaultMaxSize);
  }

  private static Attachments getExistingAttachment(
      Map<String, Object> keys, List<Attachments> existingAttachments) {
    return existingAttachments.stream()
        .filter(existingData -> ApplicationHandlerHelper.areKeysInData(keys, existingData))
        .findAny()
        .orElse(Attachments.create());
  }

  private ModifyApplicationHandlerHelper() {
    // avoid instantiation
  }
}

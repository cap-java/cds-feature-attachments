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
import com.sap.cds.feature.attachments.handler.common.AttachmentContext;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
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
          AttachmentContext context =
              AttachmentContext.from(path.target().type(), element);
          return handleAttachmentForEntity(
              condensedExistingAttachments,
              eventFactory,
              eventContext,
              path,
              (InputStream) value,
              defaultMaxSize,
              context);
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
   * @param context the attachment context describing how to address this attachment's fields
   * @return the processed content as an {@link InputStream}
   */
  public static InputStream handleAttachmentForEntity(
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      Path path,
      InputStream content,
      String defaultMaxSize,
      AttachmentContext context) {
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
    ReadonlyDataContextEnhancer.restoreReadonlyFields((CdsData) path.target().values());
    Attachments attachment = getExistingAttachment(keys, existingAttachments, context);

    String contentId =
        (String) path.target().values().get(context.fieldName(Attachments.CONTENT_ID));

    String contentLength = eventContext.getParameterInfo().getHeader("Content-Length");
    String maxSizeStr = getValMaxValue(path.target().entity(), defaultMaxSize, context);
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
      return eventToProcess.processEvent(path, wrappedContent, attachment, eventContext, context);
    } catch (Exception e) {
      if (wrappedContent != null && wrappedContent.isLimitExceeded()) {
        throw tooLargeException;
      }
      throw e;
    }
  }

  private static String getValMaxValue(
      CdsEntity entity, String defaultMaxSize, AttachmentContext context) {
    Optional<CdsElement> contentElement =
        entity.findElement(context.fieldName("content"));
    return contentElement
        .flatMap(e -> e.findAnnotation("Validation.Maximum"))
        .map(CdsAnnotation::getValue)
        .filter(v -> !"true".equals(v.toString()))
        .map(Object::toString)
        .orElse(defaultMaxSize);
  }

  private static Attachments getExistingAttachment(
      Map<String, Object> keys,
      List<Attachments> existingAttachments,
      AttachmentContext context) {
    return existingAttachments.stream()
        .filter(existingData -> context.matches(existingData, keys))
        .findAny()
        .orElse(Attachments.create());
  }

  private ModifyApplicationHandlerHelper() {
    // avoid instantiation
  }
}

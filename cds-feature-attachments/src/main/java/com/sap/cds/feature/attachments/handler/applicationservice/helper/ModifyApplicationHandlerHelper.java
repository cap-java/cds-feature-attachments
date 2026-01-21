/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.CountingInputStream;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ModifyApplicationHandlerHelper {

  private static final Filter VALMAX_FILTER = (path, element, type) -> element.getName().contentEquals("content")
      && element.findAnnotation("Validation.Maximum").isPresent();

  /**
   * Handles attachments for entities.
   *
   * @param entity              the {@link CdsEntity entity} to handle attachments
   *                            for
   * @param data                the given list of {@link CdsData data}
   * @param existingAttachments the given list of existing {@link CdsData data}
   * @param eventFactory        the {@link ModifyAttachmentEventFactory} to create
   *                            the corresponding event
   * @param eventContext        the current {@link EventContext}
   */
  public static void handleAttachmentForEntities(
      CdsEntity entity,
      List<CdsData> data,
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext) {
    Converter converter = (path, element, value) -> handleAttachmentForEntity(
        existingAttachments, eventFactory, eventContext, path, (InputStream) value);

    CdsDataProcessor.create()
        .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
        .process(data, entity);
  }

  /**
   * Handles attachments for a single entity.
   *
   * @param existingAttachments the list of existing {@link Attachments} to check
   *                            against
   * @param eventFactory        the {@link ModifyAttachmentEventFactory} to create
   *                            the corresponding event
   * @param eventContext        the current {@link EventContext}
   * @param path                the {@link Path} of the attachment
   * @param content             the content of the attachment
   * @return the processed content as an {@link InputStream}
   */
  public static InputStream handleAttachmentForEntity(
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext,
      Path path,
      InputStream content) {
    Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
    ReadonlyDataContextEnhancer.restoreReadonlyFields((CdsData) path.target().values());
    Attachments attachment = getExistingAttachment(keys, existingAttachments);
    String contentId = (String) path.target().values().get(Attachments.CONTENT_ID);
    String contentLength = eventContext.getParameterInfo().getHeader("Content-Length");
    String maxSizeStr = getValMaxValue(path.target().entity(), existingAttachments);
    if (maxSizeStr == null) {
        maxSizeStr = Long.toString(Long.MAX_VALUE);
    }
    eventContext.put("attachment.MaxSize", maxSizeStr); // make max size available in context for error handling later
    ServiceException TOO_LARGE_EXCEPTION = new ServiceException(ExtendedErrorStatuses.CONTENT_TOO_LARGE,
        "AttachmentSizeExceeded", maxSizeStr);

    if (contentLength != null) {
      try {
        if (Long.parseLong(contentLength) > FileSizeUtils.parseFileSizeToBytes(maxSizeStr)) {
          throw TOO_LARGE_EXCEPTION;
        }
      } catch (NumberFormatException e) {
        throw new ServiceException(com.sap.cds.services.ErrorStatuses.BAD_REQUEST, 
            "Invalid Content-Length header");
      }
    }
      throw TOO_LARGE_EXCEPTION;
    }
    CountingInputStream wrappedContent = content != null ? new CountingInputStream(content, maxSizeStr) : null;
    ModifyAttachmentEvent eventToProcess = eventFactory.getEvent(wrappedContent,
        contentId, attachment);
    try {
      return eventToProcess.processEvent(path, wrappedContent, attachment, eventContext);
    } catch (Exception e) {
      if (wrappedContent != null && wrappedContent.isLimitExceeded()) {
        throw TOO_LARGE_EXCEPTION;
      }
      throw e;
    }
  }

  private static String getValMaxValue(CdsEntity entity, List<? extends CdsData> data) {
    AtomicReference<String> annotationValue = new AtomicReference<>();
    CdsDataProcessor.create()
        .addValidator(
            VALMAX_FILTER,
            (path, element, value) -> {
              element
                  .findAnnotation("Validation.Maximum")
                  .ifPresent(annotation -> annotationValue.set(annotation.getValue().toString()));
            })
        .process(data, entity);
    return annotationValue.get();
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

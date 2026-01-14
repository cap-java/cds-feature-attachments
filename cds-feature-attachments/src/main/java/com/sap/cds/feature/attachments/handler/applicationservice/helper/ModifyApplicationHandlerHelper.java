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
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ModifyApplicationHandlerHelper {

  private static final Filter VALMAX_FILTER =
      (path, element, type) ->
          element.getName().contentEquals("content")
              && element.findAnnotation("Validation.Maximum").isPresent();

  /**
   * Handles attachments for entities.
   *
   * @param entity the {@link CdsEntity entity} to handle attachments for
   * @param data the given list of {@link CdsData data}
   * @param existingAttachments the given list of existing {@link CdsData data}
   * @param eventFactory the {@link ModifyAttachmentEventFactory} to create the corresponding event
   * @param eventContext the current {@link EventContext}
   */
  public static void handleAttachmentForEntities(
      CdsEntity entity,
      List<CdsData> data,
      List<Attachments> existingAttachments,
      ModifyAttachmentEventFactory eventFactory,
      EventContext eventContext) {
    Converter converter =
        (path, element, value) ->
            handleAttachmentForEntity(
                existingAttachments, eventFactory, eventContext, path, (InputStream) value);

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

    if (maxSizeStr != null && content != null) {
      try {
        long maxSize = FileSizeUtils.parseFileSizeToBytes(maxSizeStr);
        if (contentLength != null && Long.parseLong(contentLength) > maxSize) {
          throw new RuntimeException("File size exceeds the maximum allowed size of " + maxSizeStr);
        }
      } catch (ArithmeticException e) {
        throw new ServiceException("Maximum file size value is too large", e);
      } catch (RuntimeException e) {
        throw new ServiceException(
            ExtendedErrorStatuses.CONTENT_TOO_LARGE, "AttachmentSizeExceeded", maxSizeStr);
      } catch (Exception e) {
        throw new ServiceException(
            ErrorStatuses.BAD_REQUEST, "Failed to process attachment size", e);
      }
    }

    // for the current request find the event to process
    ModifyAttachmentEvent eventToProcess = eventFactory.getEvent(content, contentId, attachment);

    // process the event
    return eventToProcess.processEvent(path, content, attachment, eventContext);
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

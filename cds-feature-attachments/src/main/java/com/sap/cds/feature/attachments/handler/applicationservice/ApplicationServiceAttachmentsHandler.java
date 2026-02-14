/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ExtendedErrorStatuses;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ReadonlyDataContextEnhancer;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentEntityScanner;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.utils.OrderConstants;
import com.sap.cds.services.utils.model.CqnUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified event handler for attachment operations on application services. Handles CREATE, READ,
 * UPDATE, and DELETE events for entities with attachments.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ApplicationServiceAttachmentsHandler implements EventHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(ApplicationServiceAttachmentsHandler.class);

  // Dependencies for CREATE/UPDATE operations
  private final ModifyAttachmentEventFactory eventFactory;
  private final ThreadDataStorageReader storageReader;
  private final String defaultMaxSize;

  // Dependencies for READ operations
  private final AttachmentService attachmentService;
  private final AttachmentStatusValidator statusValidator;
  private final AsyncMalwareScanExecutor scanExecutor;

  // Dependencies for UPDATE/DELETE operations
  private final AttachmentsReader attachmentsReader;
  private final AttachmentService outboxedAttachmentService;
  private final MarkAsDeletedAttachmentEvent deleteEvent;

  public ApplicationServiceAttachmentsHandler(
      ModifyAttachmentEventFactory eventFactory,
      ThreadDataStorageReader storageReader,
      String defaultMaxSize,
      AttachmentService attachmentService,
      AttachmentStatusValidator statusValidator,
      AsyncMalwareScanExecutor scanExecutor,
      AttachmentsReader attachmentsReader,
      AttachmentService outboxedAttachmentService,
      MarkAsDeletedAttachmentEvent deleteEvent) {
    this.eventFactory = requireNonNull(eventFactory, "eventFactory must not be null");
    this.storageReader = requireNonNull(storageReader, "storageReader must not be null");
    this.defaultMaxSize = requireNonNull(defaultMaxSize, "defaultMaxSize must not be null");
    this.attachmentService =
        requireNonNull(attachmentService, "attachmentService must not be null");
    this.statusValidator = requireNonNull(statusValidator, "statusValidator must not be null");
    this.scanExecutor = requireNonNull(scanExecutor, "scanExecutor must not be null");
    this.attachmentsReader =
        requireNonNull(attachmentsReader, "attachmentsReader must not be null");
    this.outboxedAttachmentService =
        requireNonNull(outboxedAttachmentService, "outboxedAttachmentService must not be null");
    this.deleteEvent = requireNonNull(deleteEvent, "deleteEvent must not be null");
  }

  // ========== CREATE Event Handlers ==========

  @Before(event = CqnService.EVENT_CREATE)
  @HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
  void preserveReadonlyFieldsOnCreate(CdsCreateEventContext context, List<CdsData> data) {
    ReadonlyDataContextEnhancer.preserveReadonlyFields(
        context.getTarget(), data, storageReader.get());
  }

  @Before(event = CqnService.EVENT_CREATE)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeCreate(CdsCreateEventContext context, List<CdsData> data) {
    if (ApplicationHandlerHelper.containsContentField(context.getTarget(), data)) {
      logger.debug(
          "Processing before {} event for entity {}", context.getEvent(), context.getTarget());
      ModifyApplicationHandlerHelper.handleAttachmentForEntities(
          context.getTarget(), data, new ArrayList<>(), eventFactory, context, defaultMaxSize);
    }
  }

  @On(event = {CqnService.EVENT_CREATE, CqnService.EVENT_UPDATE, DraftService.EVENT_DRAFT_PATCH})
  @HandlerOrder(HandlerOrder.EARLY)
  void handleContentTooLargeError(EventContext context) {
    try {
      context.proceed();
    } catch (ServiceException e) {
      if (e.getErrorStatus() == ExtendedErrorStatuses.CONTENT_TOO_LARGE) {
        String maxSizeStr = (String) context.get("attachment.MaxSize");
        if (maxSizeStr != null) {
          throw new ServiceException(
              ExtendedErrorStatuses.CONTENT_TOO_LARGE, "AttachmentSizeExceeded", maxSizeStr);
        }
        throw new ServiceException(
            ExtendedErrorStatuses.CONTENT_TOO_LARGE, "AttachmentSizeExceeded");
      }
      throw e;
    }
  }

  // ========== READ Event Handlers ==========

  @Before(event = CqnService.EVENT_READ)
  @HandlerOrder(HandlerOrder.EARLY)
  void processBeforeRead(CdsReadEventContext context) {
    logger.debug("Processing before {} for entity {}.", context.getEvent(), context.getTarget());

    List<String> fieldNames =
        AttachmentEntityScanner.getAttachmentAssociationNames(context.getTarget());
    if (!fieldNames.isEmpty()) {
      CqnSelect resultCqn = CQL.copy(context.getCqn(), new BeforeReadItemsModifier(fieldNames));
      context.setCqn(resultCqn);
    }
  }

  @After(event = CqnService.EVENT_READ)
  @HandlerOrder(HandlerOrder.EARLY)
  void processAfterRead(CdsReadEventContext context, List<CdsData> data) {
    if (ApplicationHandlerHelper.containsContentField(context.getTarget(), data)) {
      logger.debug(
          "Processing after {} event for entity {}", context.getEvent(), context.getTarget());

      Converter converter =
          (path, element, value) -> {
            Attachments attachment = Attachments.of(path.target().values());
            InputStream content = attachment.getContent();
            if (nonNull(attachment.getContentId())) {
              verifyStatus(path, attachment);
              Supplier<InputStream> supplier =
                  nonNull(content)
                      ? () -> content
                      : () -> attachmentService.readAttachment(attachment.getContentId());
              return new LazyProxyInputStream(supplier, statusValidator, attachment.getStatus());
            } else {
              return value;
            }
          };

      CdsDataProcessor.create()
          .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
          .process(data, context.getTarget());
    }
  }

  private void verifyStatus(Path path, Attachments attachment) {
    if (areKeysEmpty(path.target().keys())) {
      String currentStatus = attachment.getStatus();
      logger.debug(
          "In verify status for content id {} and status {}",
          attachment.getContentId(),
          currentStatus);
      if (StatusCode.UNSCANNED.equals(currentStatus)
          || StatusCode.SCANNING.equals(currentStatus)
          || currentStatus == null) {
        logger.debug(
            "Scanning content with ID {} for malware, has current status {}",
            attachment.getContentId(),
            currentStatus);
        scanExecutor.scanAsync(path.target().entity(), attachment.getContentId());
      }
      statusValidator.verifyStatus(attachment.getStatus());
    }
  }

  private boolean areKeysEmpty(Map<String, Object> keys) {
    return keys.values().stream().allMatch(Objects::isNull);
  }

  // ========== UPDATE Event Handlers ==========

  @Before(event = CqnService.EVENT_UPDATE)
  @HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
  void preserveReadonlyFieldsOnUpdate(CdsUpdateEventContext context, List<CdsData> data) {
    ReadonlyDataContextEnhancer.preserveReadonlyFields(
        context.getTarget(), data, storageReader.get());
  }

  @Before(event = CqnService.EVENT_UPDATE)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeUpdate(CdsUpdateEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    boolean associationsAreUnchanged = associationsAreUnchanged(target, data);
    boolean containsContent = ApplicationHandlerHelper.containsContentField(target, data);

    if (containsContent || !associationsAreUnchanged) {
      logger.debug("Processing before {} event for entity {}", context.getEvent(), target);

      CqnSelect select = CqnUtils.toSelect(context.getCqn(), context.getTarget());
      List<Attachments> attachments =
          attachmentsReader.readAttachments(context.getModel(), target, select);

      ModifyApplicationHandlerHelper.handleAttachmentForEntities(
          target, data, attachments, eventFactory, context, defaultMaxSize);

      if (!associationsAreUnchanged) {
        deleteRemovedAttachments(attachments, data, target, context.getUserInfo());
      }
    }
  }

  private boolean associationsAreUnchanged(CdsEntity entity, List<CdsData> data) {
    return entity
        .compositions()
        .noneMatch(
            association -> data.stream().anyMatch(d -> d.containsKey(association.getName())));
  }

  private void deleteRemovedAttachments(
      List<Attachments> dbAttachments,
      List<CdsData> requestData,
      CdsEntity entity,
      UserInfo userInfo) {
    List<Attachments> condensedDbAttachments =
        ApplicationHandlerHelper.condenseAttachments(dbAttachments, entity);
    List<Attachments> requestAttachments =
        ApplicationHandlerHelper.condenseAttachments(requestData, entity);

    for (Attachments dbAttachment : condensedDbAttachments) {
      Map<String, Object> dbKeys = ApplicationHandlerHelper.extractKeys(dbAttachment, entity);
      Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(dbKeys);

      boolean existsInRequest =
          requestAttachments.stream()
              .anyMatch(
                  requestAttachment ->
                      ApplicationHandlerHelper.areKeysInData(keys, requestAttachment));

      if (!existsInRequest && dbAttachment.getContentId() != null) {
        outboxedAttachmentService.markAttachmentAsDeleted(
            new MarkAsDeletedInput(dbAttachment.getContentId(), userInfo));
      }
    }
  }

  // ========== DELETE Event Handlers ==========

  @Before(event = CqnService.EVENT_DELETE)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeDelete(CdsDeleteEventContext context) {
    logger.debug(
        "Processing before {} event for entity {}", context.getEvent(), context.getTarget());

    List<Attachments> attachments =
        attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn());

    Converter converter =
        (path, element, value) ->
            deleteEvent.processEvent(
                path, (InputStream) value, Attachments.of(path.target().values()), context);

    CdsDataProcessor.create()
        .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
        .process(attachments, context.getTarget());
  }
}

/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ReadonlyDataContextEnhancer;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentCountValidator;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.utils.OrderConstants;
import com.sap.cds.services.utils.model.CqnUtils;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link UpdateAttachmentsHandler} is an event handler that is called before an update
 * event is executed. As updates in draft entities or non-draft entities can also be create-events,
 * update-events or delete-events the handler needs to distinguish between the different cases.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class UpdateAttachmentsHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(UpdateAttachmentsHandler.class);

  private final ModifyAttachmentEventFactory eventFactory;
  private final AttachmentsReader attachmentsReader;
  private final AttachmentService attachmentService;
  private final ThreadDataStorageReader storageReader;
  private final AttachmentCountValidator validator;

  public UpdateAttachmentsHandler(
      ModifyAttachmentEventFactory eventFactory,
      AttachmentsReader attachmentsReader,
      AttachmentService attachmentService,
      ThreadDataStorageReader storageReader,
      AttachmentCountValidator validator) {
    this.eventFactory = requireNonNull(eventFactory, "eventFactory must not be null");
    this.attachmentsReader =
        requireNonNull(attachmentsReader, "attachmentsReader must not be null");
    this.attachmentService =
        requireNonNull(attachmentService, "attachmentService must not be null");
    this.storageReader = requireNonNull(storageReader, "storageReader must not be null");
    this.validator = requireNonNull(validator, "validator must not be null");
  }

  @Before
  @HandlerOrder(OrderConstants.Before.CHECK_CAPABILITIES)
  void processBeforeForDraft(CdsUpdateEventContext context, List<CdsData> data) {
    // before the attachment's readonly fields are removed by the runtime, preserve
    // them in a custom
    // field in data
    ReadonlyDataContextEnhancer.preserveReadonlyFields(
        context.getTarget(), data, storageReader.get());
  }

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void processBefore(CdsUpdateEventContext context, List<CdsData> data) {

    CdsEntity target = context.getTarget();
    boolean associationsAreUnchanged = associationsAreUnchanged(target, data);
    boolean containsContent = ApplicationHandlerHelper.containsContentField(target, data);

    if (containsContent || !associationsAreUnchanged) {
      logger.debug("Processing before {} event for entity {}", context.getEvent(), target);
      validator.validateForUpdate(target, data);

      // Query database only for validation (single query for all attachments)
      CqnSelect select = CqnUtils.toSelect(context.getCqn(), context.getTarget());
      List<Attachments> attachments =
          attachmentsReader.readAttachments(context.getModel(), target, select);

      ModifyApplicationHandlerHelper.handleAttachmentForEntities(
          target, data, attachments, eventFactory, context);

      if (!associationsAreUnchanged) {
        deleteRemovedAttachments(attachments, data, target, context.getUserInfo());
      }
    }
  }

  private boolean associationsAreUnchanged(CdsEntity entity, List<CdsData> data) {
    // TODO: check if this should be replaced with
    // entity.assocations().noneMatch(...)
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
    // Condense both dbAttachments and requestData to get flat lists of actual attachment entities
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
        attachmentService.markAttachmentAsDeleted(
            new MarkAsDeletedInput(dbAttachment.getContentId(), userInfo));
      }
    }
  }
}

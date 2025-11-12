/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link DraftCancelAttachmentsHandler} is an event handler that is called before a draft
 * cancel event is executed. The handler checks if the attachments of the draft entity are still
 * valid and deletes the content of the attachments if necessary.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftCancelAttachmentsHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(DraftCancelAttachmentsHandler.class);

  private static final Filter contentIdFilter =
      (path, element, type) ->
          ApplicationHandlerHelper.isMediaEntity(path.target().type())
              && element.getName().equals(Attachments.CONTENT_ID);

  private final AttachmentsReader attachmentsReader;
  private final MarkAsDeletedAttachmentEvent deleteEvent;

  public DraftCancelAttachmentsHandler(
      AttachmentsReader attachmentsReader, MarkAsDeletedAttachmentEvent deleteEvent) {
    this.attachmentsReader =
        requireNonNull(attachmentsReader, "attachmentsReader must not be null");
    this.deleteEvent = requireNonNull(deleteEvent, "deleteEvent must not be null");
  }

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeDraftCancel(DraftCancelEventContext context) {
    // We only process the draft cancel event if there is no WHERE clause in the CQN
    // and if the target entity is an attachment entity or has attachment associations.
    if ((isAttachmentEntity(context.getTarget()) || hasAttachmentAssociations(context.getTarget()))
        && isWhereEmpty(context)) {
      logger.debug(
          "Processing before {} event for entity {}", context.getEvent(), context.getTarget());

      CdsEntity activeEntity = DraftUtils.getActiveEntity(context.getTarget());
      CdsEntity draftEntity = DraftUtils.getDraftEntity(context.getTarget());

      List<Attachments> draftAttachments = readAttachments(context, draftEntity, false);
      List<Attachments> activeCondensedAttachments =
          getCondensedActiveAttachments(context, activeEntity);

      Validator validator = buildDeleteContentValidator(context, activeCondensedAttachments);
      CdsDataProcessor.create()
          .addValidator(contentIdFilter, validator)
          .process(draftAttachments, context.getTarget());
    }
  }

  private Validator buildDeleteContentValidator(
      DraftCancelEventContext context, List<? extends CdsData> activeCondensedAttachments) {
    return (path, element, value) -> {
      Attachments attachment = Attachments.of(path.target().values());
      if (Boolean.FALSE.equals(attachment.get(Drafts.HAS_ACTIVE_ENTITY))) {
        deleteEvent.processEvent(path, null, attachment, context);
        return;
      }
      Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
      Optional<? extends CdsData> existingEntry =
          activeCondensedAttachments.stream()
              .filter(updatedData -> ApplicationHandlerHelper.areKeysInData(keys, updatedData))
              .findAny();
      existingEntry.ifPresent(
          entry -> {
            if (!entry.get(Attachments.CONTENT_ID).equals(value)) {
              deleteEvent.processEvent(null, null, attachment, context);
            }
          });
    };
  }

  // This function checks if the WHERE clause of the CQN is empty.
  // This is the current way to verify that we are really cancelling a draft and not doing sth else.
  // Also see here:
  // https://github.com/cap-java/cds-feature-attachments/blob/main/doc/Design.md#events
  // Unfortunately, context.getEvent() does not return a reliable value in this case.
  private boolean isWhereEmpty(DraftCancelEventContext context) {
    return context.getCqn().where().isEmpty();
  }

  // This function checks if the given entity is of type Attachments
  private boolean isAttachmentEntity(CdsEntity entity) {
    boolean hasAttachmentInName = entity.getQualifiedName().toLowerCase().contains("attachment");

    boolean hasFileNameElement =
        entity.elements().anyMatch(element -> Attachments.FILE_NAME.equals(element.getName()));

    logger.debug(
        "Entity: {}, hasAttachmentInName: {}, hasFileNameElement: {}",
        entity.getQualifiedName(),
        hasAttachmentInName,
        hasFileNameElement);

    return hasAttachmentInName || hasFileNameElement;
  }

  // This function checks if the given entity has attachment associations.
  private boolean hasAttachmentAssociations(CdsEntity entity) {
    return entity
        .elements()
        .anyMatch(element -> element.getName().toLowerCase().contains("attachment"));
  }

  private List<Attachments> readAttachments(
      DraftCancelEventContext context, CdsStructuredType entity, boolean isActiveEntity) {
    logger.debug(
        "Reading attachments for entity {} (isActiveEntity={})", entity.getName(), isActiveEntity);
    logger.debug("Original CQN: {}", context.getCqn());
    CqnDelete modifiedCQN =
        CQL.copy(
            context.getCqn(),
            new ModifierToCreateFlatCQN(isActiveEntity, entity.getQualifiedName()));
    logger.debug("Modified CQN: {}", modifiedCQN);
    return attachmentsReader.readAttachments(context.getModel(), (CdsEntity) entity, modifiedCQN);
  }

  private List<Attachments> getCondensedActiveAttachments(
      DraftCancelEventContext context, CdsStructuredType activeEntity) {
    List<Attachments> attachments = readAttachments(context, activeEntity, true);
    return ApplicationHandlerHelper.condenseAttachments(attachments, context.getTarget());
  }
}

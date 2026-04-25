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
      (path, element, type) -> {
        // Case 1: Composition-based attachment entity
        if (ApplicationHandlerHelper.isDirectMediaEntity(path.target().type())
            && element.getName().equals(Attachments.CONTENT_ID)) {
          return true;
        }
        // Case 2: Inline attachment type — check for prefixed contentId
        String elementName = element.getName();
        if (elementName.endsWith("_" + Attachments.CONTENT_ID)) {
          return ApplicationHandlerHelper.getInlineAttachmentPrefix(
                  path.target().type(), elementName)
              .isPresent();
        }
        return false;
      };

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
    CdsEntity entity = context.getTarget();

    CdsEntity draftEntity = DraftUtils.getDraftEntity(entity);

    List<Attachments> draftAttachments = readAttachments(context, draftEntity, false);

    if (draftAttachments.isEmpty()) {
      logger.debug(
          "Skipping processing before {} event for entity {}",
          context.getEvent(),
          context.getTarget());
      return;
    }

    logger.debug(
        "Processing before {} event for entity {}", context.getEvent(), context.getTarget());

    CdsEntity activeEntity = DraftUtils.getActiveEntity(entity);
    List<Attachments> activeCondensedAttachments =
        getCondensedActiveAttachments(context, activeEntity);

    Validator validator = buildDeleteContentValidator(context, activeCondensedAttachments);
    CdsDataProcessor.create()
        .addValidator(contentIdFilter, validator)
        .process(draftAttachments, context.getTarget());
  }

  private Validator buildDeleteContentValidator(
      DraftCancelEventContext context, List<? extends CdsData> activeCondensedAttachments) {
    return (path, element, value) -> {
      Optional<String> inlinePrefix =
          ApplicationHandlerHelper.getInlineAttachmentPrefix(
              path.target().entity(), element.getName());

      Attachments attachment;
      if (inlinePrefix.isPresent()) {
        attachment =
            ApplicationHandlerHelper.extractInlineAttachment(
                path.target().values(), inlinePrefix.get());
        Object hasActiveEntity = path.target().values().get(Drafts.HAS_ACTIVE_ENTITY);
        if (hasActiveEntity != null) {
          attachment.put(Drafts.HAS_ACTIVE_ENTITY, hasActiveEntity);
        }
      } else {
        attachment = Attachments.of(path.target().values());
      }

      if (Boolean.FALSE.equals(attachment.get(Drafts.HAS_ACTIVE_ENTITY))) {
        deleteEvent.processEvent(path, null, attachment, context, inlinePrefix);
        return;
      }
      Map<String, Object> keys = ApplicationHandlerHelper.removeDraftKey(path.target().keys());
      Optional<? extends CdsData> existingEntry =
          activeCondensedAttachments.stream()
              .filter(updatedData -> ApplicationHandlerHelper.areKeysInData(keys, updatedData))
              .findAny();
      existingEntry.ifPresent(
          entry -> {
            if (!entry.get(Attachments.CONTENT_ID).equals(attachment.getContentId())) {
              deleteEvent.processEvent(null, null, attachment, context, inlinePrefix);
            }
          });
    };
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

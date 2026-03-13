/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.handler.applicationservice.ItemCountValidationHandler;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftNewEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Message.Severity;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link DraftItemCountValidationHandler} validates item count constraints on
 * compositions annotated with {@code @Validation.MaxItems} and {@code @Validation.MinItems} during
 * draft lifecycle events.
 *
 * <p>During DRAFT_SAVE (activation to active entity), violations produce ERRORs to reject the
 * request. During DRAFT_PATCH and DRAFT_NEW, violations produce WARNINGs since drafts allow invalid
 * state by design.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftItemCountValidationHandler implements EventHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(DraftItemCountValidationHandler.class);

  private final PersistenceService persistenceService;

  public DraftItemCountValidationHandler(PersistenceService persistenceService) {
    this.persistenceService =
        requireNonNull(persistenceService, "persistenceService must not be null");
  }

  /**
   * Validates item count constraints during DRAFT_SAVE (draft activation). This produces ERROR
   * messages because the draft is being promoted to an active entity and must be valid.
   *
   * <p>Note: The DRAFT_SAVE event does not provide request data as a handler parameter. The draft
   * data must be read from the draft tables via {@link PersistenceService}.
   */
  @Before(event = DraftService.EVENT_DRAFT_SAVE)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeDraftSave(DraftSaveEventContext context) {
    CdsEntity target = context.getTarget();
    logger.debug("Validating item count on DRAFT_SAVE for entity {}", target.getQualifiedName());

    List<CdsData> draftData = readDraftDataWithCompositions(context, target);
    if (draftData.isEmpty()) {
      logger.debug("No draft data found for entity {}", target.getQualifiedName());
      return;
    }

    ItemCountValidationHandler.validateCompositionItemCounts(
        target, draftData, null, context.getMessages(), Severity.ERROR);
  }

  /**
   * Validates item count constraints during DRAFT_PATCH. This produces WARNING messages because
   * drafts are allowed to be in an invalid state during editing.
   */
  @Before(event = DraftService.EVENT_DRAFT_PATCH)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeDraftPatch(DraftPatchEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    logger.debug("Checking item count on DRAFT_PATCH for entity {}", target.getQualifiedName());

    ItemCountValidationHandler.validateCompositionItemCounts(
        target, data, null, context.getMessages(), Severity.WARNING);
  }

  /**
   * Validates item count constraints during DRAFT_NEW. This produces WARNING messages because
   * drafts are allowed to be in an invalid state during editing.
   */
  @Before(event = DraftService.EVENT_DRAFT_NEW)
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeDraftNew(DraftNewEventContext context, List<CdsData> data) {
    CdsEntity target = context.getTarget();
    logger.debug("Checking item count on DRAFT_NEW for entity {}", target.getQualifiedName());

    ItemCountValidationHandler.validateCompositionItemCounts(
        target, data, null, context.getMessages(), Severity.WARNING);
  }

  /**
   * Reads the draft data for the given entity including annotated composition expansions. This is
   * needed for DRAFT_SAVE because the event does not provide request data.
   */
  private List<CdsData> readDraftDataWithCompositions(
      DraftSaveEventContext context, CdsEntity target) {

    // Build select from the draft entity's CQN with expanded annotated compositions
    CqnSelect contextCqn = context.getCqn();
    var selectBuilder = Select.from(contextCqn.ref());
    contextCqn.where().ifPresent(selectBuilder::where);

    // Expand annotated compositions so we can count items
    target
        .elements()
        .filter(ItemCountValidationHandler::isAnnotatedComposition)
        .forEach(element -> selectBuilder.columns(CQL.to(element.getName()).expand(CQL.star())));

    Result result = persistenceService.run(selectBuilder);
    return result.listOf(CdsData.class);
  }
}

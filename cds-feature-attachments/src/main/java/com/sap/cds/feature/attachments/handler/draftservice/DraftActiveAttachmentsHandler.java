/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ItemCountValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageSetter;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceName(value = "*", type = DraftService.class)
public class DraftActiveAttachmentsHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(DraftActiveAttachmentsHandler.class);

  private final ThreadDataStorageSetter threadLocalSetter;
  private final PersistenceService persistence;

  public DraftActiveAttachmentsHandler(
      ThreadDataStorageSetter threadLocalSetter, PersistenceService persistence) {
    this.threadLocalSetter =
        requireNonNull(threadLocalSetter, "threadLocalSetter must not be null");
    this.persistence = requireNonNull(persistence, "persistence must not be null");
  }

  /**
   * Before draft save: validate min/max items as errors. Draft save = activating the draft, so an
   * invalid state is no longer tolerated and must result in an error.
   */
  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void validateItemCountBeforeSave(DraftSaveEventContext context) {
    CdsEntity entity = context.getTarget();

    boolean hasItemCountAnnotations =
        entity.compositions().anyMatch(ItemCountValidator::hasItemCountAnnotation);
    if (!hasItemCountAnnotations) {
      return;
    }

    logger.debug(
        "Validating item count before draft save for entity {}", entity.getQualifiedName());

    CdsEntity draftEntity = DraftUtils.getDraftEntity(entity);
    List<CdsData> syntheticData = readDraftCompositionCounts(draftEntity, context, entity);
    ItemCountValidator.validate(entity, syntheticData, context, false);
  }

  @On
  void processDraftSave(DraftSaveEventContext context) {
    threadLocalSetter.set(true, context::proceed);
  }

  /**
   * Reads the draft composition data for all annotated compositions and builds a synthetic data
   * list suitable for {@link ItemCountValidator#validate}.
   *
   * <p>A SELECT with expand for each annotated composition is executed against the draft table. The
   * result is a single root {@link CdsData} entry whose composition arrays contain all found child
   * rows – this allows {@code ItemCountValidator} to count them correctly.
   */
  private List<CdsData> readDraftCompositionCounts(
      CdsEntity draftEntity, DraftSaveEventContext context, CdsEntity activeEntity) {
    List<CdsElement> annotatedComps =
        activeEntity.compositions().filter(ItemCountValidator::hasItemCountAnnotation).toList();

    var expandColumns =
        annotatedComps.stream().map(comp -> CQL.to(comp.getName()).expand()).toList();

    // context.getCqn() is the CqnSelect that identifies the draft root entity (with keys/where).
    // We re-build a SELECT on the draft entity adding the expand columns for compositions.
    var baseCqn = context.getCqn();
    Select<?> select = Select.from(draftEntity).columns(expandColumns);
    baseCqn.where().ifPresent(select::where);

    var result = persistence.run(select);
    List<CdsData> rows = result.listOf(CdsData.class);

    // Aggregate: collect all composition items across root rows into one synthetic entry.
    // In practice draft save targets a single root entity, but we sum across all for safety.
    CdsData aggregated = CdsData.create();
    for (CdsElement comp : annotatedComps) {
      List<CdsData> allItems = new ArrayList<>();
      for (CdsData row : rows) {
        Object items = row.get(comp.getName());
        if (items instanceof List<?> list) {
          list.forEach(item -> allItems.add(CdsData.create()));
        }
      }
      aggregated.put(comp.getName(), allItems);
    }
    return List.of(aggregated);
  }
}

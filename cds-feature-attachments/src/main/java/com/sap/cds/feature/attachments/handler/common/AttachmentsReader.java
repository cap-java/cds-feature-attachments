/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static java.util.Objects.requireNonNull;

import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link AttachmentsReader} is used to deep read attachments from the database for a
 * determined path from the given entity to the media entity. The class uses the {@link
 * AssociationCascader} to find the entity path.
 *
 * <p>The returned data is deep including the path structure to the media entity.
 */
public class AttachmentsReader {

  private static final Logger logger = LoggerFactory.getLogger(AttachmentsReader.class);

  private final AssociationCascader cascader;
  private final PersistenceService persistence;

  public AttachmentsReader(AssociationCascader cascader, PersistenceService persistence) {
    this.cascader = requireNonNull(cascader, "cascader must not be null");
    this.persistence = requireNonNull(persistence, "persistence must not be null");
  }

  public List<Attachments> readAttachments(
      CdsModel model, CdsEntity entity, CqnFilterableStatement statement) {
    logger.debug("Start reading attachments for entity {}", entity.getQualifiedName());

    NodeTree nodePath = cascader.findEntityPath(model, entity);
    List<Expand<?>> expandList = buildExpandList(model, nodePath);

    List<CqnSelectListItem> inlineColumns = buildInlineAttachmentColumns(entity);

    if (expandList.isEmpty()
        && inlineColumns.isEmpty()
        && !ApplicationHandlerHelper.isMediaEntity(entity)) {
      logResultData(entity, List.of());
      return List.of();
    }

    Select<?> select;
    if (!expandList.isEmpty() || !inlineColumns.isEmpty()) {
      List<CqnSelectListItem> allItems = new ArrayList<>(inlineColumns);
      allItems.addAll(expandList);
      select = Select.from(statement.ref()).columns(allItems);
    } else {
      select = Select.from(statement.ref()).columns(StructuredType::_all);
    }
    statement.where().ifPresent(select::where);

    Result result = persistence.run(select);
    List<Attachments> attachments = result.listOf(Attachments.class);
    logResultData(entity, attachments);
    return attachments;
  }

  private List<CqnSelectListItem> buildInlineAttachmentColumns(CdsEntity entity) {
    List<String> inlineFields = ApplicationHandlerHelper.getInlineAttachmentFieldNames(entity);
    List<CqnSelectListItem> columns = new ArrayList<>();
    for (String fieldName : inlineFields) {
      // Include the content field so CdsDataProcessor's MEDIA_CONTENT_FILTER can match it
      columns.add(CQL.get(fieldName + "_content"));
      columns.add(CQL.get(fieldName + "_" + Attachments.CONTENT_ID));
      columns.add(CQL.get(fieldName + "_" + Attachments.STATUS));
    }
    if (!columns.isEmpty() && entity.findElement(Drafts.HAS_ACTIVE_ENTITY).isPresent()) {
      columns.add(CQL.get(Drafts.HAS_ACTIVE_ENTITY));
    }
    return columns;
  }

  private List<Expand<?>> buildExpandList(CdsModel model, NodeTree root) {
    List<Expand<?>> expandResultList = new ArrayList<>();
    root.getChildren().forEach(child -> expandResultList.add(buildExpandFromTree(model, child)));

    return expandResultList;
  }

  private Expand<?> buildExpandFromTree(CdsModel model, NodeTree node) {
    // Look up the entity for this node to check for inline attachments
    CdsEntity nodeEntity = model.findEntity(node.getIdentifier().fullEntityName()).orElse(null);

    // Build inline attachment columns for this child entity if it has any
    List<CqnSelectListItem> inlineColumns =
        nodeEntity != null ? buildInlineAttachmentColumns(nodeEntity) : List.of();

    // Build child expands recursively
    List<CqnSelectListItem> childExpands = new ArrayList<>();
    for (NodeTree child : node.getChildren()) {
      childExpands.add(buildExpandFromTree(model, child));
    }

    // Combine inline columns and child expands
    List<CqnSelectListItem> expandItems = new ArrayList<>(inlineColumns);
    expandItems.addAll(childExpands);

    return expandItems.isEmpty()
        ? CQL.to(node.getIdentifier().associationName()).expand()
        : CQL.to(node.getIdentifier().associationName()).expand(expandItems);
  }

  private static void logResultData(CdsEntity entity, List<Attachments> attachments) {
    logger.debug(
        "Read attachments for entity {}: lines {}", entity.getQualifiedName(), attachments.size());
    if (logger.isTraceEnabled()) {
      attachments.forEach(data -> logger.trace("Read attachment data: {}", data));
    }
  }
}

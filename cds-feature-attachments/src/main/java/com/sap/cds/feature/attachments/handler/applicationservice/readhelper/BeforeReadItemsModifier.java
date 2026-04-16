/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.common.InlineAttachmentHelper;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.ql.cqn.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link BeforeReadItemsModifier} is a modifier that adds the content id field, status
 * code and scanned-at timestamp to the select items.
 */
public class BeforeReadItemsModifier implements Modifier {

  private static final Logger logger = LoggerFactory.getLogger(BeforeReadItemsModifier.class);

  private static final String ROOT_ASSOCIATION = "";

  /**
   * Prefix used to mark inline attachment entries in the media associations list. Entries of the
   * form {@code "inline:prefix"} indicate that the prefix refers to an inline (flattened)
   * attachment field rather than a composition association.
   */
  public static final String INLINE_PREFIX = "inline:";

  private final List<String> mediaAssociations;

  public BeforeReadItemsModifier(List<String> mediaAssociations) {
    this.mediaAssociations = mediaAssociations;
  }

  @Override
  public List<CqnSelectListItem> items(List<CqnSelectListItem> items) {
    List<CqnSelectListItem> newItems =
        new ArrayList<>(items.stream().filter(item -> !item.isExpand()).toList());
    List<CqnSelectListItem> result = addContentIdItem(items);
    newItems.addAll(result);

    enhanceWithInlineFields(items, newItems);

    return newItems;
  }

  private List<CqnSelectListItem> addContentIdItem(List<CqnSelectListItem> list) {
    List<CqnSelectListItem> newItems = new ArrayList<>();
    enhanceWithNewFieldForMediaAssociation(ROOT_ASSOCIATION, list, newItems);

    List<CqnSelectListItem> expandedItems =
        list.stream().filter(CqnSelectListItem::isExpand).toList();
    newItems.addAll(processExpandedEntities(expandedItems));
    return newItems;
  }

  private List<CqnSelectListItem> processExpandedEntities(List<CqnSelectListItem> expandedItems) {
    List<CqnSelectListItem> newItems = new ArrayList<>();

    expandedItems.forEach(
        item -> {
          List<CqnSelectListItem> newItemsFromExpand =
              new ArrayList<>(item.asExpand().items().stream().filter(i -> !i.isExpand()).toList());
          enhanceWithNewFieldForMediaAssociation(
              item.asExpand().displayName(), newItemsFromExpand, newItemsFromExpand);
          List<CqnSelectListItem> expandedSubItems =
              item.asExpand().items().stream().filter(CqnSelectListItem::isExpand).toList();
          List<CqnSelectListItem> result = processExpandedEntities(expandedSubItems);
          newItemsFromExpand.addAll(result);
          Expand<?> copy = CQL.copy(item.asExpand());
          copy.items(newItemsFromExpand);
          newItems.add(copy);
        });

    return newItems;
  }

  private void enhanceWithNewFieldForMediaAssociation(
      String association, List<CqnSelectListItem> list, List<CqnSelectListItem> listToEnhance) {
    if (isMediaAssociationAndNeedNewContentIdField(association, list)) {
      logger.debug("Adding document id, status code and scanned-at timestamp to select items");
      listToEnhance.add(CQL.get(Attachments.CONTENT_ID));
      listToEnhance.add(CQL.get(Attachments.STATUS));
      listToEnhance.add(CQL.get(Attachments.SCANNED_AT));
    }
  }

  private void enhanceWithInlineFields(
      List<CqnSelectListItem> originalItems, List<CqnSelectListItem> newItems) {
    for (String entry : mediaAssociations) {
      if (!entry.startsWith(INLINE_PREFIX)) {
        continue;
      }
      String prefix = entry.substring(INLINE_PREFIX.length());
      String contentFieldName = InlineAttachmentHelper.buildInlineFieldName(prefix, "content");
      String contentIdFieldName =
          InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.CONTENT_ID);

      boolean hasContentField =
          originalItems.stream().anyMatch(item -> isItemRefFieldWithName(item, contentFieldName));
      boolean hasContentIdField =
          originalItems.stream().anyMatch(item -> isItemRefFieldWithName(item, contentIdFieldName));

      if (hasContentField && !hasContentIdField) {
        logger.debug("Adding inline attachment fields for prefix '{}' to select items", prefix);
        newItems.add(
            CQL.get(InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.CONTENT_ID)));
        newItems.add(
            CQL.get(InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.STATUS)));
        newItems.add(
            CQL.get(InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.SCANNED_AT)));
      }
    }
  }

  private boolean isMediaAssociationAndNeedNewContentIdField(
      String association, List<CqnSelectListItem> list) {
    return mediaAssociations.contains(association)
        && list.stream().anyMatch(item -> isItemRefFieldWithName(item, MediaData.CONTENT))
        && list.stream().noneMatch(item -> isItemRefFieldWithName(item, Attachments.CONTENT_ID));
  }

  private boolean isItemRefFieldWithName(CqnSelectListItem item, String fieldName) {
    return item.isRef() && item.asRef().displayName().equals(fieldName);
  }
}

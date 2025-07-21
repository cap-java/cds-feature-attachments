/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Expand;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.ql.cqn.Modifier;

/**
 * The class {@link BeforeReadItemsModifier} is a modifier that adds the content id field and status code to the select
 * items.
 */
public class BeforeReadItemsModifier implements Modifier {

	private static final Logger logger = LoggerFactory.getLogger(BeforeReadItemsModifier.class);

	private static final String ROOT_ASSOCIATION = "";

	private final List<String> mediaAssociations;

	public BeforeReadItemsModifier(List<String> mediaAssociations) {
		this.mediaAssociations = mediaAssociations;
	}

	@Override
	public List<CqnSelectListItem> items(List<CqnSelectListItem> items) {
		List<CqnSelectListItem> newItems = new ArrayList<>(items.stream().filter(item -> !item.isExpand()).toList());
		List<CqnSelectListItem> result = addContentIdItem(items);
		newItems.addAll(result);

		return newItems;
	}

	private List<CqnSelectListItem> addContentIdItem(List<CqnSelectListItem> list) {
		List<CqnSelectListItem> newItems = new ArrayList<>();
		enhanceWithNewFieldForMediaAssociation(ROOT_ASSOCIATION, list, newItems);

		List<CqnSelectListItem> expandedItems = list.stream().filter(CqnSelectListItem::isExpand).toList();
		newItems.addAll(processExpandedEntities(expandedItems));
		return newItems;
	}

	private List<CqnSelectListItem> processExpandedEntities(List<CqnSelectListItem> expandedItems) {
		List<CqnSelectListItem> newItems = new ArrayList<>();

		expandedItems.forEach(item -> {
			List<CqnSelectListItem> newItemsFromExpand = new ArrayList<>(
					item.asExpand().items().stream().filter(i -> !i.isExpand()).toList());
			enhanceWithNewFieldForMediaAssociation(item.asExpand().displayName(), newItemsFromExpand,
					newItemsFromExpand);
			List<CqnSelectListItem> expandedSubItems = item.asExpand().items().stream()
					.filter(CqnSelectListItem::isExpand).toList();
			List<CqnSelectListItem> result = processExpandedEntities(expandedSubItems);
			newItemsFromExpand.addAll(result);
			Expand<?> copy = CQL.copy(item.asExpand());
			copy.items(newItemsFromExpand);
			newItems.add(copy);
		});

		return newItems;
	}

	private void enhanceWithNewFieldForMediaAssociation(String association, List<CqnSelectListItem> list,
			List<CqnSelectListItem> listToEnhance) {
		if (isMediaAssociationAndNeedNewContentIdField(association, list)) {
			logger.debug("Adding document id and status code to select items");
			listToEnhance.add(CQL.get(Attachments.CONTENT_ID));
			listToEnhance.add(CQL.get(Attachments.STATUS));
		}
	}

	private boolean isMediaAssociationAndNeedNewContentIdField(String association, List<CqnSelectListItem> list) {
		return mediaAssociations.contains(association)
				&& list.stream().anyMatch(item -> isItemRefFieldWithName(item, MediaData.CONTENT))
				&& list.stream().noneMatch(item -> isItemRefFieldWithName(item, Attachments.CONTENT_ID));
	}

	private boolean isItemRefFieldWithName(CqnSelectListItem item, String fieldName) {
		return item.isRef() && item.asRef().displayName().equals(fieldName);
	}

}

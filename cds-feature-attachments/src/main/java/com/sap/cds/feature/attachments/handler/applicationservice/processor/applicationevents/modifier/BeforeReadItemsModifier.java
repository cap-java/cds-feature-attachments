package com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.modifier;

import java.util.ArrayList;
import java.util.List;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.ql.cqn.Modifier;

public class BeforeReadItemsModifier implements Modifier {

	private static final String ROOT_ASSOCIATION = "";

	private final List<String> mediaAssociations;

	public BeforeReadItemsModifier(List<String> mediaAssociations) {
		this.mediaAssociations = mediaAssociations;
	}

	@Override
	public List<CqnSelectListItem> items(List<CqnSelectListItem> items) {
		List<CqnSelectListItem> newItems = new ArrayList<>(items.stream().filter(item -> !item.isExpand()).toList());
		var result = addDocumentIdItem(items);
		newItems.addAll(result);

		return newItems;
	}

	private List<CqnSelectListItem> addDocumentIdItem(List<CqnSelectListItem> list) {
		List<CqnSelectListItem> newItems = new ArrayList<>();
		enhanceWithNewFieldForMediaAssociation(ROOT_ASSOCIATION, list, newItems);

		List<CqnSelectListItem> expandedItems = list.stream().filter(CqnSelectListItem::isExpand).toList();
		newItems.addAll(processExpandedEntities(expandedItems));
		return newItems;
	}

	private List<CqnSelectListItem> processExpandedEntities(List<CqnSelectListItem> expandedItems) {
		List<CqnSelectListItem> newItems = new ArrayList<>();

		expandedItems.forEach(item -> {
			List<CqnSelectListItem> newItemsFromExpand = new ArrayList<>(item.asExpand().items().stream()
																																																																		.filter(i -> !i.isExpand()).toList());
			enhanceWithNewFieldForMediaAssociation(item.asExpand().displayName(), newItemsFromExpand, newItemsFromExpand);
			List<CqnSelectListItem> expandedSubItems = item.asExpand().items().stream().filter(CqnSelectListItem::isExpand)
																																																.toList();
			var result = processExpandedEntities(expandedSubItems);
			newItemsFromExpand.addAll(result);
			var copy = CQL.copy(item.asExpand());
			copy.items(newItemsFromExpand);
			newItems.add(copy);
		});

		return newItems;
	}

	private void enhanceWithNewFieldForMediaAssociation(String association, List<CqnSelectListItem> list, List<CqnSelectListItem> listToEnhance) {
		if (isMediaAssociationAndNeedNewDocumentIdField(association, list)) {
			listToEnhance.add(CQL.get(Attachments.DOCUMENT_ID));
			listToEnhance.add(CQL.get(Attachments.STATUS_CODE));
		}
	}

	private boolean isMediaAssociationAndNeedNewDocumentIdField(String association, List<CqnSelectListItem> list) {
		return mediaAssociations.contains(association) && list.stream()
																																																						.anyMatch(item -> isItemRefFieldWithName(item, MediaData.CONTENT)) && list.stream()
																																																																																																																														.noneMatch(item -> isItemRefFieldWithName(item, Attachments.DOCUMENT_ID));
	}

	private boolean isItemRefFieldWithName(CqnSelectListItem item, String fieldName) {
		return item.isRef() && item.asRef().displayName().equals(fieldName);
	}

}

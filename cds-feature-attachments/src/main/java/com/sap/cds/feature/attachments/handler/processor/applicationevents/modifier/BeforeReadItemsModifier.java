package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.cds4j.com.sap.attachments.MediaData;
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
		var fieldOptional = getNewFieldForMediaAssociation(ROOT_ASSOCIATION, list);
		fieldOptional.ifPresent(newItems::add);

		List<CqnSelectListItem> expandedItems = list.stream().filter(CqnSelectListItem::isExpand).toList();
		newItems.addAll(processExpandedEntities(expandedItems));
		return newItems;
	}

	private List<CqnSelectListItem> processExpandedEntities(List<CqnSelectListItem> expandedItems) {
		List<CqnSelectListItem> newItems = new ArrayList<>();

		expandedItems.forEach(item -> {
			List<CqnSelectListItem> newItemsFromExpand = new ArrayList<>(item.asExpand().items().stream().filter(i -> !i.isExpand()).toList());
			var fieldOptional = getNewFieldForMediaAssociation(item.asExpand().displayName(), newItemsFromExpand);
			fieldOptional.ifPresent(newItemsFromExpand::add);

			List<CqnSelectListItem> expandedSubItems = item.asExpand().items().stream().filter(CqnSelectListItem::isExpand).toList();
			var result = processExpandedEntities(expandedSubItems);
			newItemsFromExpand.addAll(result);
			var copy = CQL.copy(item.asExpand());
			copy.items(newItemsFromExpand);
			newItems.add(copy);
		});

		return newItems;
	}

	private Optional<CqnSelectListItem> getNewFieldForMediaAssociation(String association, List<CqnSelectListItem> list) {
		if (isMediaAssociationAndNeedNewDocumentIdField(association, list)) {
			return Optional.of(CQL.get(Attachments.DOCUMENT_ID));
		}
		return Optional.empty();
	}

	private boolean isMediaAssociationAndNeedNewDocumentIdField(String association, List<CqnSelectListItem> list) {
		return mediaAssociations.contains(association) && list.stream().anyMatch(item -> isItemRefFieldWithName(item, MediaData.CONTENT)) &&
											list.stream().noneMatch(item -> isItemRefFieldWithName(item, Attachments.DOCUMENT_ID));
	}

	private boolean isItemRefFieldWithName(CqnSelectListItem item, String fieldName) {
		return item.isRef() && item.asRef().displayName().equals(fieldName);
	}

}

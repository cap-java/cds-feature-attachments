package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.DocumentFieldNames;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.ql.cqn.Modifier;

public class BeforeReadItemsModifier implements Modifier {

		private final Map<String, DocumentFieldNames> associationNameMap;

		public BeforeReadItemsModifier(Map<String, DocumentFieldNames> associationNameMap) {
				this.associationNameMap = associationNameMap;
		}

		@Override
		public List<CqnSelectListItem> items(List<CqnSelectListItem> items) {
				List<CqnSelectListItem> newItems = new ArrayList<>(items.stream().filter(item -> !item.isExpand()).toList());
				var result = addDocumentIdItem("", items);
				newItems.addAll(result);

				return newItems;
		}

		private List<CqnSelectListItem> addDocumentIdItem(String association, List<CqnSelectListItem> list) {
				List<CqnSelectListItem> newItems = new ArrayList<>();
				var fieldOptional = findNewField(association, list);
				fieldOptional.ifPresent(newItems::add);

				List<CqnSelectListItem> expendedItems = list.stream().filter(CqnSelectListItem::isExpand).toList();
				newItems.addAll(processExpandedEntities(expendedItems));
				return newItems;
		}

		private Optional<CqnSelectListItem> findNewField(String association, List<CqnSelectListItem> list) {
				if (associationNameMap.containsKey(association)) {
						var fieldName = associationNameMap.get(association);
						if (list.stream().anyMatch(item -> !item.isExpand() && item.isRef() && item.asRef().displayName().equals(fieldName.contentFieldName()))) {
								return Optional.of(CQL.get(fieldName.documentIdFieldName()));
						}
				}
				return Optional.empty();
		}

		private List<CqnSelectListItem> processExpandedEntities(List<CqnSelectListItem> expendedItems) {
				List<CqnSelectListItem> newItems = new ArrayList<>();

				expendedItems.forEach(item -> {
						List<CqnSelectListItem> allItems = new ArrayList<>(item.asExpand().items().stream().filter(i -> !i.isExpand()).toList());
						var fieldOptional = findNewField(item.asExpand().displayName(), allItems);
						fieldOptional.ifPresent(allItems::add);

						List<CqnSelectListItem> expandedItems = allItems.stream().filter(CqnSelectListItem::isExpand).toList();
						var result = processExpandedEntities(expandedItems);
						allItems.addAll(result);
						var copy = CQL.copy(item.asExpand());
						copy.items(allItems);
						newItems.add(copy);
				});

				return newItems;
		}

}

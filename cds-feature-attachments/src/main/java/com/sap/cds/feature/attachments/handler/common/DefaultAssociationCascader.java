package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.ql.cqn.CqnReference.Segment;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;

public class DefaultAssociationCascader implements AssociationCascader {

	@Override
	public List<LinkedList<AssociationIdentifier>> findEntityPath(CdsModel model, CdsEntity entity, Filter filter) {
		var resultList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var firstList = new LinkedList<AssociationIdentifier>();
		resultList.add(firstList);

		var internalResultList = getAttachmentAssociationPath(model, entity, "", firstList, new ArrayList<>());
		resultList.addAll(internalResultList);

		return resultList;
	}

	private ArrayList<LinkedList<AssociationIdentifier>> getAttachmentAssociationPath(CdsModel model, CdsEntity entity, String associationName, LinkedList<AssociationIdentifier> firstList, List<String> processedEntities) {
		var internalResultList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var currentList = new AtomicReference<LinkedList<AssociationIdentifier>>();
		currentList.set(firstList);
		var query = entity.query();
		List<String> entityNames = query.map(cqnSelect -> cqnSelect.from().asRef().segments().stream().map(Segment::id).toList()).orElseGet(() -> List.of(entity.getQualifiedName()));

		var needNewMapEntry = new AtomicReference<Boolean>();
		needNewMapEntry.set(false);
		entityNames.forEach(name -> {
			var baseEntity = model.findEntity(name);
			baseEntity.ifPresent(base -> {
				if (isMediaEntity(base)) {
					var identifier = new AssociationIdentifier(associationName, name, true);
					if (needNewMapEntry.get()) {
						internalResultList.add(currentList.get());
						currentList.set(new LinkedList<AssociationIdentifier>());
						currentList.get().addLast(identifier);
					} else {
						currentList.get().addLast(identifier);
					}
				}
				needNewMapEntry.set(true);
			});
		});

		//TODO refactor with map method in stream
		Map<String, CdsEntity> annotatedEntitiesMap = new HashMap<>();
		entity.elements().filter(element -> element.getType().isAssociation()).forEach(element -> annotatedEntitiesMap.put(element.getName(), element.getType().as(CdsAssociationType.class).getTarget()));

		if (annotatedEntitiesMap.isEmpty()) {
			return internalResultList;
		}

		var newListNeeded = false;
		for (var associatedElement : annotatedEntitiesMap.entrySet()) {
			if (!processedEntities.contains(associatedElement.getKey())) {
				processedEntities.add(associatedElement.getKey());
				if (newListNeeded) {
					internalResultList.add(currentList.get());
					currentList.set(new LinkedList<AssociationIdentifier>());
				}
				newListNeeded = true;
				var result = getAttachmentAssociationPath(model, associatedElement.getValue(), associatedElement.getKey(), currentList.get(), processedEntities);
				internalResultList.addAll(result);
			}
		}

		return internalResultList;
	}

	protected boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

}

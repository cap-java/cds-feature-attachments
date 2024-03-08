package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.ql.cqn.CqnReference.Segment;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;

public class DefaultAssociationCascader implements AssociationCascader {

	@Override
	public List<LinkedList<AssociationIdentifier>> findEntityPath(CdsModel model, CdsEntity entity) {
		var firstList = new LinkedList<AssociationIdentifier>();
		var internalResultList = getAttachmentAssociationPath(model, entity, "", firstList, new ArrayList<>());

		return new ArrayList<>(internalResultList);
	}

	//TODO refactor and harmonize with ReadAttachmentsHandler
	private List<LinkedList<AssociationIdentifier>> getAttachmentAssociationPath(CdsModel model, CdsEntity entity, String associationName, LinkedList<AssociationIdentifier> firstList, List<String> processedEntities) {
		var internalResultList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var currentList = new AtomicReference<LinkedList<AssociationIdentifier>>();
		currentList.set(new LinkedList<>());

		var query = entity.query();
		Optional<String> entityNames = query.map(cqnSelect -> cqnSelect.from().asRef().segments().stream().map(Segment::id).findFirst()).orElseGet(() -> Optional.of(entity.getQualifiedName()));

		var isMediaType = new AtomicReference<Boolean>();
		isMediaType.set(false);
		entityNames.ifPresent(name -> {
			var baseEntity = model.findEntity(name);
			baseEntity.ifPresent(base -> {
				isMediaType.set(isMediaEntity(base));
				if (isMediaType.get()) {
					var identifier = new AssociationIdentifier(associationName, name, isMediaType.get());
					firstList.addLast(identifier);
				}
			});
		});
		if (isMediaType.get()) {
			internalResultList.add(firstList);
			return internalResultList;
		}

		Map<String, CdsEntity> associations = entity.elements().filter(element -> element.getType().isAssociation()).collect(Collectors.toMap(CdsElementDefinition::getName, element -> element.getType().as(CdsAssociationType.class).getTarget()));

		if (associations.isEmpty()) {
			return internalResultList;
		}

		var newListNeeded = false;
		for (var associatedElement : associations.entrySet()) {
			if (!processedEntities.contains(associatedElement.getKey())) {
				processedEntities.add(associatedElement.getKey());
				if (newListNeeded) {
					currentList.set(new LinkedList<>());
					currentList.get().addAll(firstList);
				} else {
					firstList.add(new AssociationIdentifier(associationName, entity.getQualifiedName(), false));
					currentList.get().addAll(firstList);
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

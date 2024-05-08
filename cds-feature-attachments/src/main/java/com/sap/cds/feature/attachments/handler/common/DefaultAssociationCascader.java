/**************************************************************************
	* (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
	**************************************************************************/
package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.handler.common.model.AssociationIdentifier;
import com.sap.cds.feature.attachments.handler.common.model.NodeTree;
import com.sap.cds.feature.attachments.handler.constants.ModelConstants;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;

/**
	* The class {@link DefaultAssociationCascader} is used to find entity paths
	* to all media resource entities for a given data model.
	* The path information is returned in a node tree which starts from the given entity.
	* Only composition associations are considered.
	*/
public class DefaultAssociationCascader implements AssociationCascader {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAssociationCascader.class);

	@Override
	public NodeTree findEntityPath(CdsModel model, CdsEntity entity) {
		logger.debug("Start finding path to attachments for entity {}", entity.getQualifiedName());
		var firstList = new LinkedList<AssociationIdentifier>();
		var internalResultList = getAttachmentAssociationPath(model, entity, "", firstList,
				new ArrayList<>(List.of(entity.getQualifiedName())));

		var rootTree = new NodeTree(new AssociationIdentifier("", entity.getQualifiedName()));
		internalResultList.forEach(rootTree::addPath);

		logger.debug("Found path to attachments for entity {}: {}", entity.getQualifiedName(), rootTree);
		return rootTree;
	}

	private List<LinkedList<AssociationIdentifier>> getAttachmentAssociationPath(CdsModel model, CdsEntity entity,
			String associationName, LinkedList<AssociationIdentifier> firstList, List<String> processedEntities) {
		var internalResultList = new ArrayList<LinkedList<AssociationIdentifier>>();
		var currentList = new AtomicReference<LinkedList<AssociationIdentifier>>();
		var localProcessEntities = new ArrayList<String>();
		currentList.set(new LinkedList<>());

		var baseEntity = ApplicationHandlerHelper.getBaseEntity(model, entity);
		var isMediaEntity = isMediaEntity(baseEntity);
		if (isMediaEntity) {
			var identifier = new AssociationIdentifier(associationName, entity.getQualifiedName());
			firstList.addLast(identifier);
		}

		if (isMediaEntity) {
			internalResultList.add(firstList);
			return internalResultList;
		}

		Map<String, CdsEntity> associations = entity.elements().filter(
						element -> element.getType().isAssociation() && element.getType().as(CdsAssociationType.class).isComposition())
				.collect(Collectors.toMap(CdsElementDefinition::getName,
						element -> element.getType().as(CdsAssociationType.class).getTarget()));

		if (associations.isEmpty()) {
			return internalResultList;
		}

		var newListNeeded = false;
		for (var associatedElement : associations.entrySet()) {
			if (!processedEntities.contains(associatedElement.getValue().getQualifiedName())) {
				if (newListNeeded) {
					currentList.set(new LinkedList<>());
					currentList.get().addAll(firstList);
					processedEntities = localProcessEntities;
				} else {
					firstList.add(new AssociationIdentifier(associationName, entity.getQualifiedName()));
					currentList.get().addAll(firstList);
					localProcessEntities = new ArrayList<>(processedEntities);
				}
				processedEntities.add(associatedElement.getValue().getQualifiedName());
				newListNeeded = true;
				var result = getAttachmentAssociationPath(model, associatedElement.getValue(), associatedElement.getKey(),
						currentList.get(), processedEntities);
				internalResultList.addAll(result);
			}
		}

		return internalResultList;
	}

	private boolean isMediaEntity(CdsStructuredType baseEntity) {
		return baseEntity.getAnnotationValue(ModelConstants.ANNOTATION_IS_MEDIA_DATA, false);
	}

}

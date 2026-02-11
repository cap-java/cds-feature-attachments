/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link AssociationCascader} is used to find entity paths to all media resource entities
 * for a given data model. The path information is returned in a node tree which starts from the
 * given entity. Only composition associations are considered.
 */
public class AssociationCascader {

  private static final Logger logger = LoggerFactory.getLogger(AssociationCascader.class);

  public NodeTree findEntityPath(CdsModel model, CdsEntity entity) {
    logger.debug("Start finding path to attachments for entity {}", entity.getQualifiedName());
    var firstList = new LinkedList<AssociationIdentifier>();
    var internalResultList =
        getAttachmentAssociationPath(
            model, entity, "", firstList, new ArrayList<>(List.of(entity.getQualifiedName())));

    var rootTree = new NodeTree(new AssociationIdentifier("", entity.getQualifiedName()));
    internalResultList.forEach(rootTree::addPath);

    logger.debug(
        "Found path to attachments for entity {}: {}", entity.getQualifiedName(), rootTree);
    return rootTree;
  }

  private List<LinkedList<AssociationIdentifier>> getAttachmentAssociationPath(
      CdsModel model,
      CdsEntity entity,
      String associationName,
      LinkedList<AssociationIdentifier> firstList,
      List<String> processedEntities) {
    var internalResultList = new ArrayList<LinkedList<AssociationIdentifier>>();
    var currentList = new AtomicReference<LinkedList<AssociationIdentifier>>();
    var localProcessEntities = new ArrayList<String>();
    currentList.set(new LinkedList<>());

    var isMediaEntity = ApplicationHandlerHelper.isMediaEntity(entity);
    if (isMediaEntity) {
      var identifier = new AssociationIdentifier(associationName, entity.getQualifiedName());
      firstList.addLast(identifier);
    }

    if (isMediaEntity) {
      internalResultList.add(firstList);
      return internalResultList;
    }

    Map<String, CdsEntity> compositions = AttachmentEntityScanner.getCompositions(entity);

    if (compositions.isEmpty()) {
      return internalResultList;
    }

    var newListNeeded = false;
    for (Map.Entry<String, CdsEntity> compositionEntry : compositions.entrySet()) {
      if (!processedEntities.contains(compositionEntry.getValue().getQualifiedName())) {
        if (newListNeeded) {
          currentList.set(new LinkedList<>());
          currentList.get().addAll(firstList);
          processedEntities = localProcessEntities;
        } else {
          firstList.add(new AssociationIdentifier(associationName, entity.getQualifiedName()));
          currentList.get().addAll(firstList);
          localProcessEntities = new ArrayList<>(processedEntities);
        }
        processedEntities.add(compositionEntry.getValue().getQualifiedName());
        newListNeeded = true;
        var result =
            getAttachmentAssociationPath(
                model,
                compositionEntry.getValue(),
                compositionEntry.getKey(),
                currentList.get(),
                processedEntities);
        internalResultList.addAll(result);
      }
    }

    return internalResultList;
  }
}

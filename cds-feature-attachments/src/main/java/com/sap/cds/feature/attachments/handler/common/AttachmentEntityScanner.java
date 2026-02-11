/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElementDefinition;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.Drafts;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for scanning CDS entities to find attachment-related associations and compositions.
 * Consolidates entity traversal logic previously duplicated across multiple handlers.
 */
public final class AttachmentEntityScanner {

  private AttachmentEntityScanner() {
    // prevent instantiation
  }

  /**
   * Checks if the given entity or any of its compositions contains attachment entities.
   *
   * @param entity the entity to check
   * @return {@code true} if attachments are found in the entity hierarchy, {@code false} otherwise
   */
  public static boolean hasAttachments(CdsEntity entity) {
    return hasAttachmentsRecursive(entity, new HashSet<>());
  }

  /**
   * Returns the names of associations that lead to attachment entities. This includes both direct
   * associations to attachment entities and associations that eventually lead to attachments
   * through their compositions.
   *
   * @param entity the root entity to scan
   * @return list of association names leading to attachments
   */
  public static List<String> getAttachmentAssociationNames(CdsEntity entity) {
    return getAssociationNamesRecursive(entity, "", new HashSet<>());
  }

  /**
   * Returns composition associations from the given entity that are compositions (not just regular
   * associations). Used by {@link AssociationCascader} for building path trees.
   *
   * @param entity the entity to get compositions from
   * @return map of composition name to target entity
   */
  public static Map<String, CdsEntity> getCompositions(CdsEntity entity) {
    return entity
        .elements()
        .filter(
            element ->
                element.getType().isAssociation()
                    && element.getType().as(CdsAssociationType.class).isComposition())
        .collect(
            Collectors.toMap(
                CdsElementDefinition::getName,
                element -> element.getType().as(CdsAssociationType.class).getTarget()));
  }

  private static boolean hasAttachmentsRecursive(CdsEntity entity, Set<String> visited) {
    if (visited.contains(entity.getQualifiedName())) {
      return false;
    }
    visited.add(entity.getQualifiedName());

    if (ApplicationHandlerHelper.isMediaEntity(entity)) {
      return true;
    }

    return entity
        .compositions()
        .map(element -> element.getType().as(CdsAssociationType.class))
        .anyMatch(association -> hasAttachmentsRecursive(association.getTarget(), visited));
  }

  private static List<String> getAssociationNamesRecursive(
      CdsEntity entity, String associationName, Set<String> visited) {
    List<String> result = new ArrayList<>();

    if (ApplicationHandlerHelper.isMediaEntity(entity)) {
      result.add(associationName);
      return result;
    }

    Map<String, CdsEntity> associations =
        entity
            .associations()
            .collect(
                Collectors.toMap(
                    CdsElementDefinition::getName,
                    element -> element.getType().as(CdsAssociationType.class).getTarget()));

    for (Map.Entry<String, CdsEntity> entry : associations.entrySet()) {
      String name = entry.getKey();
      CdsEntity target = entry.getValue();

      if (!result.contains(name)
          && !visited.contains(name)
          && !Drafts.SIBLING_ENTITY.equals(name)) {
        visited.add(name);
        List<String> nested = getAssociationNamesRecursive(target, name, visited);
        result.addAll(nested);
      }
    }

    return result;
  }
}

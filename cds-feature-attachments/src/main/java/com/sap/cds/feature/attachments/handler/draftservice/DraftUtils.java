/*
 * © 2025-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.Drafts;

/** Utility class for draft handling. */
public class DraftUtils {

  /** The suffix for draft entities. */
  static final String DRAFT_TABLE_POSTFIX = "_drafts";

  /**
   * Returns the active entity of the given entity. If the given entity is already the active
   * entity, the entity itself is returned, otherwise the sibling entity is returned.
   *
   * @param entity the {@link CdsEntity entity} to get the active entity for
   * @return the active entity
   */
  static CdsEntity getActiveEntity(CdsEntity entity) {
    return isDraftEntity(entity) ? entity.getTargetOf(Drafts.SIBLING_ENTITY) : entity;
  }

  /**
   * Returns the draft entity of the given entity. If the given entity is already a draft entity,
   * the entity itself is returned, otherwise the sibling entity is returned.
   *
   * @param entity the {@link CdsEntity entity} to get the {@link CdsEntity draft entity} for
   * @return the draft entity
   */
  static CdsEntity getDraftEntity(CdsEntity entity) {
    return isDraftEntity(entity) ? entity : entity.getTargetOf(Drafts.SIBLING_ENTITY);
  }

  /**
   * Checks if the given target entity is a draft entity.
   *
   * @param entity the target entity
   * @return {@code true} if the given entity is a draft entity, {@code false} otherwise
   */
  static boolean isDraftEntity(CdsEntity entity) {
    return entity.getQualifiedName().endsWith(DRAFT_TABLE_POSTFIX);
  }

  private DraftUtils() {
    // avoid instantiation
  }
}

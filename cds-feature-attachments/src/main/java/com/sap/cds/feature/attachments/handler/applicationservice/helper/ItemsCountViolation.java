/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

/**
 * Represents a validation violation for items count constraints on a composition.
 *
 * @param type the type of violation (MAX_ITEMS or MIN_ITEMS)
 * @param compositionName the name of the composition association that was violated
 * @param entityName the simple name of the parent entity
 * @param actualCount the actual number of items found
 * @param limit the configured limit that was violated
 */
public record ItemsCountViolation(
    Type type, String compositionName, String entityName, int actualCount, int limit) {

  /** The type of items count violation. */
  public enum Type {
    MAX_ITEMS,
    MIN_ITEMS
  }

  /**
   * Returns the i18n message key for this violation. The key follows the Fiori elements convention
   * of appending entity name and property name for overriding.
   *
   * <p>Base keys:
   *
   * <ul>
   *   <li>{@code AttachmentMaxItemsExceeded} for MAX_ITEMS violations
   *   <li>{@code AttachmentMinItemsNotReached} for MIN_ITEMS violations
   * </ul>
   *
   * <p>Override keys (checked first):
   *
   * <ul>
   *   <li>{@code AttachmentMaxItemsExceeded_<EntityName>_<CompositionName>}
   *   <li>{@code AttachmentMinItemsNotReached_<EntityName>_<CompositionName>}
   * </ul>
   */
  public String getBaseMessageKey() {
    return type == Type.MAX_ITEMS ? "AttachmentMaxItemsExceeded" : "AttachmentMinItemsNotReached";
  }

  /**
   * Returns the entity/property-specific override message key.
   *
   * @return the override key in the form {@code <BaseKey>_<EntityName>_<CompositionName>}
   */
  public String getOverrideMessageKey() {
    return getBaseMessageKey() + "_" + entityName + "_" + compositionName;
  }
}

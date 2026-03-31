/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The class {@link ReadonlyDataContextEnhancer} provides methods to backup and restore readonly
 * fields of attachments in the data.
 */
public final class ReadonlyDataContextEnhancer {

  private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";

  /**
   * Preserves the readonly fields of an {@link Attachments attachment} in a custom field with the
   * name {@value #DRAFT_READONLY_CONTEXT}. These readonly data will be removed from the data by the
   * CAP Java runtime, but the preserved copy still exists.
   *
   * @param target the target {@link CdsEntity entity}
   * @param data the list of {@link CdsData data} to enhance
   * @param isDraft <code>true</code> if the data is from a draft entity, <code>false</code>
   *     otherwise
   */
  public static void preserveReadonlyFields(CdsEntity target, List<CdsData> data, boolean isDraft) {

    Validator validator =
        (path, element, value) -> {
          if (isDraft) {
            // Determine if this is an inline attachment field
            Optional<String> inlinePrefix =
                ApplicationHandlerHelper.getInlineAttachmentPrefix(
                    path.target().type(), element.getName());
            if (inlinePrefix.isPresent()) {
              // Inline attachment: use prefixed field names
              String prefix = inlinePrefix.get() + "_";
              Attachments attachment = Attachments.create();
              attachment.setContentId(
                  (String) path.target().values().get(prefix + Attachments.CONTENT_ID));
              attachment.setStatus(
                  (String) path.target().values().get(prefix + Attachments.STATUS));
              attachment.setScannedAt(
                  (java.time.Instant) path.target().values().get(prefix + Attachments.SCANNED_AT));
              path.target().values().put(prefix + DRAFT_READONLY_CONTEXT, attachment);
            } else {
              // Composition-based attachment: use direct field names
              Attachments values = Attachments.of(path.target().values());
              Attachments attachment = Attachments.create();
              attachment.setContentId(values.getContentId());
              attachment.setStatus(values.getStatus());
              attachment.setScannedAt(values.getScannedAt());
              path.target().values().put(DRAFT_READONLY_CONTEXT, attachment);
            }
          } else {
            path.target().values().remove(DRAFT_READONLY_CONTEXT);
            // Also remove inline prefixed draft readonly contexts
            List<String> prefixes =
                ApplicationHandlerHelper.getInlineAttachmentFieldNames(path.target().type());
            for (String prefix : prefixes) {
              path.target().values().remove(prefix + "_" + DRAFT_READONLY_CONTEXT);
            }
          }
        };

    CdsDataProcessor.create()
        .addValidator(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, validator)
        .process(data, target);
  }

  /**
   * Restores the readonly fields with the backup from the data in the custom field {@value
   * #DRAFT_READONLY_CONTEXT}. Supports both composition-based and inline attachment fields.
   *
   * @param data the {@link CdsData data} to restore with readonly fields
   */
  public static void restoreReadonlyFields(CdsData data) {
    // Restore composition-based readonly fields
    CdsData readOnlyData = (CdsData) data.get(DRAFT_READONLY_CONTEXT);
    if (Objects.nonNull(readOnlyData)) {
      data.put(Attachments.CONTENT_ID, readOnlyData.get(Attachments.CONTENT_ID));
      data.put(Attachments.STATUS, readOnlyData.get(Attachments.STATUS));
      data.put(Attachments.SCANNED_AT, readOnlyData.get(Attachments.SCANNED_AT));
      data.remove(DRAFT_READONLY_CONTEXT);
    }

    // Restore inline attachment readonly fields
    for (String key : List.copyOf(data.keySet())) {
      if (key.endsWith("_" + DRAFT_READONLY_CONTEXT)) {
        String prefix = key.substring(0, key.length() - DRAFT_READONLY_CONTEXT.length() - 1);
        CdsData inlineReadOnlyData = (CdsData) data.get(key);
        if (Objects.nonNull(inlineReadOnlyData)) {
          data.put(
              prefix + "_" + Attachments.CONTENT_ID,
              inlineReadOnlyData.get(Attachments.CONTENT_ID));
          data.put(prefix + "_" + Attachments.STATUS, inlineReadOnlyData.get(Attachments.STATUS));
          data.put(
              prefix + "_" + Attachments.SCANNED_AT,
              inlineReadOnlyData.get(Attachments.SCANNED_AT));
          data.remove(key);
        }
      }
    }
  }

  private ReadonlyDataContextEnhancer() {
    // avoid instantiation
  }
}

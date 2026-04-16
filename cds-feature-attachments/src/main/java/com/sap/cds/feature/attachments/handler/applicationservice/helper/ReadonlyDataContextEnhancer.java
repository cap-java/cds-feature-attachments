/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.InlineAttachmentHelper;
import com.sap.cds.reflect.CdsEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The class {@link ReadonlyDataContextEnhancer} provides methods to backup and restore readonly
 * fields of attachments in the data.
 */
public final class ReadonlyDataContextEnhancer {

  private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";
  private static final String INLINE_READONLY_CONTEXT = "INLINE_READONLY_CONTEXT";

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
            Attachments values = Attachments.of(path.target().values());
            Attachments attachment = Attachments.create();
            attachment.setContentId(values.getContentId());
            attachment.setStatus(values.getStatus());
            attachment.setScannedAt(values.getScannedAt());
            path.target().values().put(DRAFT_READONLY_CONTEXT, attachment);
          } else {
            path.target().values().remove(DRAFT_READONLY_CONTEXT);
          }
        };

    CdsDataProcessor.create()
        .addValidator(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, validator)
        .process(data, target);
  }

  /**
   * Restores the readonly fields with the backup from the data in the custom field {@value
   * #DRAFT_READONLY_CONTEXT}.
   *
   * @param data the {@link CdsData data} to restore with readonly fields
   */
  public static void restoreReadonlyFields(CdsData data) {
    CdsData readOnlyData = (CdsData) data.get(DRAFT_READONLY_CONTEXT);
    if (Objects.nonNull(readOnlyData)) {
      data.put(Attachments.CONTENT_ID, readOnlyData.get(Attachments.CONTENT_ID));
      data.put(Attachments.STATUS, readOnlyData.get(Attachments.STATUS));
      data.put(Attachments.SCANNED_AT, readOnlyData.get(Attachments.SCANNED_AT));
      data.remove(DRAFT_READONLY_CONTEXT);
    }
  }

  /**
   * Preserves the inline attachment readonly fields (contentId, status, scannedAt) in a custom
   * context field so they survive the runtime's readonly field stripping during draft activation.
   *
   * @param target the target entity
   * @param data the data rows to preserve fields from
   * @param isDraft true if this is a draft activation
   */
  public static void preserveInlineReadonlyFields(
      CdsEntity target, List<CdsData> data, boolean isDraft) {
    List<String> prefixes = InlineAttachmentHelper.findInlineAttachmentPrefixes(target);
    if (prefixes.isEmpty()) {
      return;
    }
    for (CdsData row : data) {
      if (isDraft) {
        Map<String, Object> backup = new HashMap<>();
        for (String prefix : prefixes) {
          String contentIdField =
              InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.CONTENT_ID);
          String statusField =
              InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.STATUS);
          String scannedAtField =
              InlineAttachmentHelper.buildInlineFieldName(prefix, Attachments.SCANNED_AT);
          if (row.containsKey(contentIdField)) {
            backup.put(contentIdField, row.get(contentIdField));
          }
          if (row.containsKey(statusField)) {
            backup.put(statusField, row.get(statusField));
          }
          if (row.containsKey(scannedAtField)) {
            backup.put(scannedAtField, row.get(scannedAtField));
          }
        }
        if (!backup.isEmpty()) {
          row.put(INLINE_READONLY_CONTEXT, backup);
        }
      } else {
        row.remove(INLINE_READONLY_CONTEXT);
      }
    }
  }

  /**
   * Restores inline attachment readonly fields that were preserved by {@link
   * #preserveInlineReadonlyFields}.
   *
   * @param data the data rows to restore inline readonly fields to
   */
  @SuppressWarnings("unchecked")
  public static void restoreInlineReadonlyFields(List<CdsData> data) {
    for (CdsData row : data) {
      Object backupObj = row.get(INLINE_READONLY_CONTEXT);
      if (backupObj instanceof Map<?, ?> backup) {
        ((Map<String, Object>) backup).forEach(row::put);
        row.remove(INLINE_READONLY_CONTEXT);
      }
    }
  }

  private ReadonlyDataContextEnhancer() {
    // avoid instantiation
  }
}

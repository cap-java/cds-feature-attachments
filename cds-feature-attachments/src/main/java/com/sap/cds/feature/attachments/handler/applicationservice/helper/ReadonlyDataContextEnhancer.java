/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentContext;
import com.sap.cds.reflect.CdsEntity;
import java.util.List;
import java.util.Objects;

/**
 * The class {@link ReadonlyDataContextEnhancer} provides methods to backup and restore readonly
 * fields of attachments in the data.
 */
public final class ReadonlyDataContextEnhancer {

  private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";

  /**
   * Preserves the readonly fields of an {@link Attachments attachment} in a custom field. These
   * readonly data will be removed from the data by the CAP Java runtime, but the preserved copy
   * still exists.
   *
   * @param target the target {@link CdsEntity entity}
   * @param data the list of {@link CdsData data} to enhance
   * @param isDraft <code>true</code> if the data is from a draft entity, <code>false</code>
   *     otherwise
   */
  public static void preserveReadonlyFields(
      CdsEntity target, List<CdsData> data, boolean isDraft) {

    Validator validator =
        (path, element, value) -> {
          AttachmentContext context =
              AttachmentContext.from(path.target().type(), element);
          if (isDraft) {
            Attachments attachment = Attachments.create();
            attachment.setContentId(
                (String) path.target().values().get(context.fieldName(Attachments.CONTENT_ID)));
            attachment.setStatus(
                (String) path.target().values().get(context.fieldName(Attachments.STATUS)));
            attachment.setScannedAt(
                (java.time.Instant)
                    path.target().values().get(context.fieldName(Attachments.SCANNED_AT)));
            attachment.setFileName(
                (String) path.target().values().get(context.fieldName(MediaData.FILE_NAME)));
            path.target().values().put(context.fieldName(DRAFT_READONLY_CONTEXT), attachment);
          } else {
            path.target().values().remove(context.fieldName(DRAFT_READONLY_CONTEXT));
          }
        };

    CdsDataProcessor.create()
        .addValidator(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, validator)
        .process(data, target);
  }

  /**
   * Restores the readonly fields for the attachment described by the given context from the
   * preserved backup in the data map.
   *
   * @param data the {@link CdsData data} to restore with readonly fields
   * @param context the attachment context identifying which attachment's fields to restore
   */
  public static void restoreReadonlyFields(CdsData data, AttachmentContext context) {
    String readonlyKey = context.fieldName(DRAFT_READONLY_CONTEXT);
    CdsData readOnlyData = (CdsData) data.get(readonlyKey);
    if (Objects.nonNull(readOnlyData)) {
      data.put(context.fieldName(Attachments.CONTENT_ID), readOnlyData.get(Attachments.CONTENT_ID));
      data.put(context.fieldName(Attachments.STATUS), readOnlyData.get(Attachments.STATUS));
      data.put(context.fieldName(Attachments.SCANNED_AT), readOnlyData.get(Attachments.SCANNED_AT));
      if (readOnlyData.get(MediaData.FILE_NAME) != null) {
        data.put(context.fieldName(MediaData.FILE_NAME), readOnlyData.get(MediaData.FILE_NAME));
      }
      data.remove(readonlyKey);
    }
  }

  private ReadonlyDataContextEnhancer() {
    // avoid instantiation
  }
}

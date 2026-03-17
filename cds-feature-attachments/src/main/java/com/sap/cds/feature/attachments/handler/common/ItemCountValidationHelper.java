/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import com.sap.cds.CdsData;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.messages.Messages;
import java.util.List;

public final class ItemCountValidationHelper {

  private static final String MIN_ITEMS_KEY = "attachment_minItems";
  private static final String MAX_ITEMS_KEY = "attachment_maxItems";

  public static void validateItemCounts(
      CdsEntity entity, List<? extends CdsData> data, boolean isDraft, Messages messages) {
    entity
        .compositions()
        .forEach(
            composition -> {
              String compositionName = composition.getName();

              // only validate if the composition was included in the payload
              boolean presentInPayload =
                  data.stream().anyMatch(d -> d.containsKey(compositionName));
              if (!presentInPayload) {
                return;
              }

              long count =
                  data.stream()
                      .filter(d -> d.containsKey(compositionName))
                      .mapToLong(
                          d -> {
                            Object val = d.get(compositionName);
                            return val instanceof List<?> list ? list.size() : 0L;
                          })
                      .sum();

              composition
                  .<Object>findAnnotation("Validation.MinItems")
                  .ifPresent(
                      annotation -> {
                        Object value = annotation.getValue();
                        if (value instanceof Boolean)
                          return; // bare annotation without value → skip
                        int limit = ((Number) value).intValue();
                        if (count < limit) {
                          String msgKey =
                              MIN_ITEMS_KEY + "_" + entity.getName() + "_" + compositionName;
                          issueMessage(
                              isDraft,
                              messages,
                              msgKey,
                              limit,
                              entity.getQualifiedName(),
                              compositionName);
                        }
                      });

              composition
                  .<Object>findAnnotation("Validation.MaxItems")
                  .ifPresent(
                      annotation -> {
                        Object value = annotation.getValue();
                        if (value instanceof Boolean)
                          return; // bare annotation without value → skip
                        int limit = ((Number) value).intValue();
                        if (count > limit) {
                          String msgKey =
                              MAX_ITEMS_KEY + "_" + entity.getName() + "_" + compositionName;
                          issueMessage(
                              isDraft,
                              messages,
                              msgKey,
                              limit,
                              entity.getQualifiedName(),
                              compositionName);
                        }
                      });
            });
  }

  private static void issueMessage(
      boolean isDraft,
      Messages messages,
      String msgKey,
      int limit,
      String entityQualifiedName,
      String compositionName) {
    if (isDraft) {
      messages.warn(msgKey, limit).target(entityQualifiedName, e -> e.to(compositionName));
    } else {
      messages.error(msgKey, limit).target(entityQualifiedName, e -> e.to(compositionName));
    }
  }

  private ItemCountValidationHelper() {
    // avoid instantiation
  }
}

/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.ItemCountValidationHelper;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.util.List;

/**
 * The class {@link ItemsCountValidationHandler} validates {@code @Validation.MinItems} and
 * {@code @Validation.MaxItems} annotations on attachment compositions during CREATE and UPDATE
 * events. In draft mode (entity not yet activated) a warning is issued; on active entities an error
 * is raised.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ItemsCountValidationHandler implements EventHandler {

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void processCreateBefore(CdsCreateEventContext context, List<CdsData> data) {
    boolean isDraft = isDraftData(data);
    ItemCountValidationHelper.validateItemCounts(
        context.getTarget(), data, isDraft, context.getMessages());
  }

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void processUpdateBefore(CdsUpdateEventContext context, List<CdsData> data) {
    boolean isDraft = isDraftData(data);
    ItemCountValidationHelper.validateItemCounts(
        context.getTarget(), data, isDraft, context.getMessages());
  }

  private boolean isDraftData(List<CdsData> data) {
    return data.stream().anyMatch(d -> Boolean.FALSE.equals(d.get("IsActiveEntity")));
  }
}

/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.common.ItemCountValidationHelper;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.util.List;

/**
 * The class {@link DraftSaveItemsCountValidationHandler} validates {@code @Validation.MinItems} and
 * {@code @Validation.MaxItems} annotations on attachment compositions when a draft is saved
 * (activated). As draft activation transitions to an active entity, violations always raise errors.
 */
@ServiceName(value = "*", type = DraftService.class)
public class DraftSaveItemsCountValidationHandler implements EventHandler {

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void processBeforeDraftSave(DraftSaveEventContext context, List<CdsData> data) {
    // isDraft=false: saving a draft to active must enforce the constraint as an error
    ItemCountValidationHelper.validateItemCounts(
        context.getTarget(), data, false, context.getMessages());
  }
}

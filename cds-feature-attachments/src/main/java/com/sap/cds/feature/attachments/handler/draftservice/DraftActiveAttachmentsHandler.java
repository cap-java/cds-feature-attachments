/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftActiveAttachmentsHandler implements EventHandler {

	public static final String IS_DRAFT = "@internal:isDraft";

	@On
	void processDraftSave(DraftSaveEventContext context) {
		context.put(IS_DRAFT, Boolean.TRUE);
		context.proceed();
	}
}

/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadLocalDataStorage;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftActiveAttachmentsHandler implements EventHandler {

	private final ThreadLocalDataStorage threadLocalSetter;

	public DraftActiveAttachmentsHandler(ThreadLocalDataStorage threadLocalSetter) {
		this.threadLocalSetter = threadLocalSetter;
	}

	@On
	public void processDraftSave(DraftSaveEventContext context) {
		threadLocalSetter.set(true, context::proceed);
	}

}

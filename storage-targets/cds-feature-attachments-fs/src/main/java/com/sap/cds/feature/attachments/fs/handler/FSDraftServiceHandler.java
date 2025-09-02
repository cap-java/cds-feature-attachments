/*
 * © 2025-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.fs.handler;

import java.net.URLConnection;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftCreateEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
 * Event handler for events on the DraftService.
 */
@ServiceName(value = "*", type = DraftService.class)
public class FSDraftServiceHandler implements EventHandler {

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	void createDraftAttachment(DraftCreateEventContext context, CdsData data) {
		CdsEntity target = context.getTarget();

		// check if target entity contains aspect Attachments
		if (ApplicationHandlerHelper.isMediaEntity(target)) {
			String fileName = (String) data.get(Attachments.FILE_NAME);

			// guessing the MIME type of the attachment based on the file name
			String mimeType = URLConnection.guessContentTypeFromName(fileName);
			data.put(Attachments.MIME_TYPE, mimeType);

			// do something with the data of the draft attachments entity
		}
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	void patchDraftAttachment(DraftPatchEventContext context, CdsData data) {
		CdsEntity target = context.getTarget();

		// check if target entity contains aspect Attachments
		if (ApplicationHandlerHelper.isMediaEntity(target)) {
			// remove wrong mime type from data
			// TODO: remove this once the SAPUI5 sets the correct MIME type
			data.remove(Attachments.MIME_TYPE);
		}
	}
}
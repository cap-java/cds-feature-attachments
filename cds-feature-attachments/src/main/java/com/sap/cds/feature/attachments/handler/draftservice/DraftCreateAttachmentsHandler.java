/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.draftservice;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.services.draft.DraftCreateEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;

@ServiceName(value = "*", type = DraftService.class)
public class DraftCreateAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DraftCreateAttachmentsHandler.class);

	@Before
	void processDraftCeate(DraftCreateEventContext context, List<CdsData> data) {
		// if there is no content field in the data, we do not need to process the attachments
		if (ApplicationHandlerHelper.noContentFieldInData(context.getTarget(), data)) {
			return;
		}
		logger.info("Target: {}, CQN: {}", context.getTarget(), context.getCqn());
		Converter converter = (path, element, value) -> {
			// remove the content field from the data, as it is not needed in the draft
			return Converter.REMOVE; 
		};
		CdsDataProcessor.create().addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter).process(data,
				context.getTarget());
	}

}

/**************************************************************************
 * (C) 2019-2025 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.handler.applicationservice;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
 * The class {@link DeleteAttachmentsHandler} is an event handler that is responsible for deleting attachments for
 * entities. It is called before a delete event is executed.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class DeleteAttachmentsHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(DeleteAttachmentsHandler.class);

	private final AttachmentsReader attachmentsReader;
	private final ModifyAttachmentEvent deleteContentAttachmentEvent;

	public DeleteAttachmentsHandler(AttachmentsReader attachmentsReader,
			ModifyAttachmentEvent deleteContentAttachmentEvent) {
		this.attachmentsReader = attachmentsReader;
		this.deleteContentAttachmentEvent = deleteContentAttachmentEvent;
	}

	@Before
	@HandlerOrder(HandlerOrder.LATE)
	public void processBefore(CdsDeleteEventContext context) {
		logger.debug("Processing before delete event for entity {}", context.getTarget().getName());

		var attachments = attachmentsReader.readAttachments(context.getModel(), context.getTarget(), context.getCqn());

		Converter converter = (path, element, value) -> deleteContentAttachmentEvent.processEvent(path,
				(InputStream) value, CdsData.create(path.target().values()), context);

		CdsDataProcessor.create().addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter).process(attachments, context.getTarget());
	}

}

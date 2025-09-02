/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link DeleteAttachmentsHandler} is an event handler that is responsible for deleting
 * attachments for entities. It is called before a delete event is executed.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class DeleteAttachmentsHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(DeleteAttachmentsHandler.class);

  private final AttachmentsReader attachmentsReader;
  private final MarkAsDeletedAttachmentEvent deleteEvent;

  public DeleteAttachmentsHandler(
      AttachmentsReader attachmentsReader, MarkAsDeletedAttachmentEvent deleteEvent) {
    this.attachmentsReader =
        requireNonNull(attachmentsReader, "attachmentsReader must not be null");
    this.deleteEvent = requireNonNull(deleteEvent, "deleteEvent must not be null");
  }

  @Before
  @HandlerOrder(HandlerOrder.LATE)
  void processBefore(CdsDeleteEventContext context) {
    logger.debug(
        "Processing before {} event for entity {}", context.getEvent(), context.getTarget());

    List<Attachments> attachments =
        attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn());

    Converter converter =
        (path, element, value) ->
            deleteEvent.processEvent(
                path, (InputStream) value, Attachments.of(path.target().values()), context);

    CdsDataProcessor.create()
        .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
        .process(attachments, context.getTarget());
  }
}

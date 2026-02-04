/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@ServiceName(value = "*", type = AttachmentService.class)
@Profile(Profiles.TEST_HANDLER_ENABLED)
@Component
public class TestPluginAttachmentsServiceHandler implements EventHandler {

  private static final Marker marker = MarkerFactory.getMarker("DUMMY_HANDLER");
  private static final Logger logger =
      LoggerFactory.getLogger(TestPluginAttachmentsServiceHandler.class);

  private static final Map<String, byte[]> documents = new ConcurrentHashMap<>();
  private static final List<EventContextHolder> eventContextHolder =
      Collections.synchronizedList(new ArrayList<>());

  @On(event = AttachmentService.EVENT_CREATE_ATTACHMENT)
  public void createAttachment(AttachmentCreateEventContext context) throws IOException {
    logger.info(marker, "CREATE Attachment called in dummy handler");
    var contentId = UUID.randomUUID().toString();
    documents.put(contentId, context.getData().getContent().readAllBytes());
    context.setContentId(contentId);
    context.getData().setStatus(StatusCode.CLEAN);
    context.setCompleted();
    eventContextHolder.add(
        new EventContextHolder(AttachmentService.EVENT_CREATE_ATTACHMENT, context));
  }

  @On(event = AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED)
  public void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) {
    logger.info(
        marker,
        "DELETE Attachment called in dummy handler for document id {}",
        context.getContentId());
    context.setCompleted();
    eventContextHolder.add(
        new EventContextHolder(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED, context));
  }

  @On(event = AttachmentService.EVENT_READ_ATTACHMENT)
  public void readAttachment(AttachmentReadEventContext context) {
    logger.info(
        marker,
        "READ Attachment called in dummy handler for content id {}",
        context.getContentId());
    var contentId = context.getContentId();
    var content = contentId != null ? documents.get(contentId) : null;
    var stream = content != null ? new ByteArrayInputStream(content) : null;
    context.getData().setContent(stream);
    context.setCompleted();
    eventContextHolder.add(
        new EventContextHolder(AttachmentService.EVENT_READ_ATTACHMENT, context));
  }

  @On(event = AttachmentService.EVENT_RESTORE_ATTACHMENT)
  public void restoreAttachment(AttachmentRestoreEventContext context) {
    logger.info(
        marker,
        "RESTORE Attachment called in dummy handler for timestamp {}",
        context.getRestoreTimestamp());
    context.setCompleted();
    eventContextHolder.add(
        new EventContextHolder(AttachmentService.EVENT_RESTORE_ATTACHMENT, context));
  }

  public List<EventContextHolder> getEventContextForEvent(String event) {
    var context = eventContextHolder.stream().filter(e -> e.event().equals(event)).toList();
    if (event.equals(AttachmentService.EVENT_CREATE_ATTACHMENT) && !context.isEmpty()) {
      context.forEach(
          c -> {
            var createContext = (AttachmentCreateEventContext) c.context();
            createContext
                .getData()
                .setContent(new ByteArrayInputStream(documents.get(createContext.getContentId())));
          });
    }
    return context;
  }

  public List<EventContextHolder> getEventContext() {
    return eventContextHolder;
  }

  public void clearEventContext() {
    eventContextHolder.clear();
  }

  public void clearDocuments() {
    eventContextHolder.clear();
  }
}

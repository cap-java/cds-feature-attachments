/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.EventContextHolder;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class DraftOdataRequestValidationWithTestHandlerTest extends DraftOdataRequestValidationBase {

  @Test
  void serviceHandlerIsNotEmpty() {
    assertThat(serviceHandler).isNotNull();
    verifyNoAttachmentEventsCalled();
  }

  @Override
  protected void verifyContentId(String contentId, String attachmentId) {
    assertThat(contentId).isNotEmpty().isNotEqualTo(attachmentId);
  }

  @Override
  protected void verifyContent(InputStream attachment, String testContent) {
    assertThat(attachment).isNull();
  }

  @Override
  protected void verifyNoAttachmentEventsCalled() {
    assertThat(serviceHandler.getEventContext()).isEmpty();
  }

  @Override
  protected void clearServiceHandlerContext() {
    serviceHandler.clearEventContext();
  }

  @Override
  protected void verifyEventContextEmptyForEvent(String... events) {
    Arrays.stream(events)
        .forEach(event -> assertThat(serviceHandler.getEventContextForEvent(event)).isEmpty());
  }

  @Override
  protected void verifyOnlyTwoCreateEvents(
      String newAttachmentContent, String newAttachmentEntityContent) {
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED,
        AttachmentService.EVENT_READ_ATTACHMENT);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(2);
    var attachmentContentFound =
        isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentContent);
    assertThat(attachmentContentFound).isTrue();
    var attachmentEntityContentFound =
        isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentEntityContent);
    assertThat(attachmentEntityContentFound).isTrue();
  }

  @Override
  protected void verifyTwoCreateAndDeleteEvents(
      String newAttachmentContent, String newAttachmentEntityContent) {
    awaitNumberOfExpectedEvents(4);
    verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(2);
    var attachmentContentFound =
        isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentContent);
    assertThat(attachmentContentFound).isTrue();
    var attachmentEntityContentFound =
        isAttachmentContentFoundInCreateEvent(createEvents, newAttachmentEntityContent);
    assertThat(attachmentEntityContentFound).isTrue();
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(2);
    deleteEvents.forEach(
        event -> {
          var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
          assertThat(deleteContext.getContentId()).isNotEmpty();
          var createEventFound =
              createEvents.stream()
                  .anyMatch(
                      createEvent -> {
                        var createContext = (AttachmentCreateEventContext) createEvent.context();
                        return createContext.getContentId().equals(deleteContext.getContentId());
                      });
          assertThat(createEventFound).isTrue();
        });
  }

  @Override
  protected void verifyTwoReadEvents() {
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED,
        AttachmentService.EVENT_CREATE_ATTACHMENT);
    var readEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
    assertThat(readEvents).hasSize(2);
  }

  @Override
  protected void verifyOnlyTwoDeleteEvents(
      String attachmentContentId, String attachmentEntityContentId) {
    awaitNumberOfExpectedEvents(2);
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_CREATE_ATTACHMENT, AttachmentService.EVENT_READ_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(2);
    verifyDeleteEventContainsContentId(deleteEvents, attachmentContentId);
    verifyDeleteEventContainsContentId(deleteEvents, attachmentEntityContentId);
  }

  @Override
  protected void verifyTwoUpdateEvents(
      String newAttachmentContent,
      String attachmentContentId,
      String newAttachmentEntityContent,
      String attachmentEntityContentId) {
    awaitNumberOfExpectedEvents(4);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(createEvents).hasSize(2);
    verifyCreateEventFound(createEvents, newAttachmentContent);
    verifyCreateEventFound(createEvents, newAttachmentEntityContent);
    assertThat(deleteEvents).hasSize(2);
    verifyDeleteEventContainsContentId(deleteEvents, attachmentContentId);
    verifyDeleteEventContainsContentId(deleteEvents, attachmentEntityContentId);
  }

  @Override
  protected void verifyTwoCreateAndRevertedDeleteEvents() {
    awaitNumberOfExpectedEvents(4);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(createEvents).hasSize(2);
    assertThat(deleteEvents).hasSize(2);
    deleteEvents.forEach(
        event -> {
          var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
          var createEventFound =
              createEvents.stream()
                  .anyMatch(
                      createEvent -> {
                        var createContext = (AttachmentCreateEventContext) createEvent.context();
                        return createContext.getContentId().equals(deleteContext.getContentId());
                      });
          assertThat(createEventFound).isTrue();
        });
  }

  private void awaitNumberOfExpectedEvents(int expectedEvents) {
    Awaitility.await()
        .atMost(20, TimeUnit.SECONDS)
        .until(() -> serviceHandler.getEventContext().size() == expectedEvents);
  }

  private void verifyCreateEventFound(List<EventContextHolder> createEvents, String newContent) {
    var eventContentFound =
        createEvents.stream()
            .anyMatch(
                event -> {
                  var createContext = (AttachmentCreateEventContext) event.context();
                  try {
                    return Arrays.equals(
                        createContext.getData().getContent().readAllBytes(),
                        newContent.getBytes(StandardCharsets.UTF_8));
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });
    assertThat(eventContentFound).isTrue();
  }

  private boolean isAttachmentContentFoundInCreateEvent(
      List<EventContextHolder> createEvents, String newAttachmentContent) {
    return createEvents.stream()
        .anyMatch(
            event -> {
              var createContext = (AttachmentCreateEventContext) event.context();
              try {
                return Arrays.equals(
                    createContext.getData().getContent().readAllBytes(),
                    newAttachmentContent.getBytes(StandardCharsets.UTF_8));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private void verifyDeleteEventContainsContentId(
      List<EventContextHolder> deleteEvents, String contentId) {
    var eventFound =
        deleteEvents.stream()
            .anyMatch(
                event -> {
                  var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
                  return deleteContext.getContentId().equals(contentId);
                });
    assertThat(eventFound).isTrue();
  }
}

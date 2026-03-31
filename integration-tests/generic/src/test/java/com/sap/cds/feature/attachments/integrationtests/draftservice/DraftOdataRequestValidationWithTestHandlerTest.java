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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class DraftOdataRequestValidationWithTestHandlerTest extends DraftOdataRequestValidationBase {

  private static final Logger logger =
      LoggerFactory.getLogger(DraftOdataRequestValidationWithTestHandlerTest.class);

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
        .atMost(60, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(
            () -> {
              var eventCalls = serviceHandler.getEventContext().size();
              logger.info(
                  "Waiting for expected size '{}' in handler context, was '{}'",
                  expectedEvents,
                  eventCalls);
              var numberMatch = eventCalls >= expectedEvents;
              if (!numberMatch) {
                serviceHandler.getEventContext().forEach(event -> logger.info("Event: {}", event));
              }
              return numberMatch;
            });
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

  // Override flaky tests from base class to disable them.
  // These tests are affected by a race condition in the CAP runtime's outbox TaskScheduler
  // where the second DELETE event is not processed when two transactions fail in quick succession.

  @Disabled("Flaky due to CAP runtime outbox race condition - second DELETE event not processed")
  @Test
  @Override
  void errorInTransactionAfterCreateCallsDelete() throws Exception {
    super.errorInTransactionAfterCreateCallsDelete();
  }

  @Disabled("Flaky due to CAP runtime outbox race condition - second DELETE event not processed")
  @Test
  @Override
  void errorInTransactionAfterCreateCallsDeleteAndNothingForCancel() throws Exception {
    super.errorInTransactionAfterCreateCallsDeleteAndNothingForCancel();
  }

  @Disabled("Flaky due to CAP runtime outbox race condition - second DELETE event not processed")
  @Test
  @Override
  void errorInTransactionAfterUpdateCallsDelete() throws Exception {
    super.errorInTransactionAfterUpdateCallsDelete();
  }

  @Disabled("Flaky due to CAP runtime outbox race condition - second DELETE event not processed")
  @Test
  @Override
  void errorInTransactionAfterUpdateCallsDeleteEvenIfDraftIsCancelled() throws Exception {
    super.errorInTransactionAfterUpdateCallsDeleteEvenIfDraftIsCancelled();
  }

  @Disabled("Flaky due to CAP runtime outbox race condition - second DELETE event not processed")
  @Test
  @Override
  void createAttachmentAndCancelDraft() throws Exception {
    super.createAttachmentAndCancelDraft();
  }

  @Disabled("Flaky due to CAP runtime outbox race condition - second DELETE event not processed")
  @Test
  @Override
  void createAndDeleteAttachmentWorks() throws Exception {
    super.createAndDeleteAttachmentWorks();
  }
}

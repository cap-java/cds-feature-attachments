/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPersistenceHandler;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.services.persistence.PersistenceService;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class SingleAttachmentDraftTest {

  private static final Logger logger = LoggerFactory.getLogger(SingleAttachmentDraftTest.class);
  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";

  @Autowired private TestPluginAttachmentsServiceHandler serviceHandler;
  @Autowired private MockHttpRequestHelper requestHelper;
  @Autowired private PersistenceService persistenceService;
  @Autowired private TableDataDeleter dataDeleter;
  @Autowired private TestPersistenceHandler testPersistenceHandler;
  @Autowired private MockMvc mvc;

  @AfterEach
  void teardown() {
    dataDeleter.deleteData(
        DraftRoots_.CDS_NAME, DraftRoots_.CDS_NAME + "_drafts", "cds.outbox.Messages");
    serviceHandler.clearEventContext();
    serviceHandler.clearDocuments();
    requestHelper.resetHelper();
    testPersistenceHandler.reset();
  }

  @Test
  void createInlineAttachmentInDraftAndActivate() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());

    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        draftRootUrl, "{\"title\":\"some title\"}");

    var content = putInlineAttachmentContent(draftRootUrl, "avatarContent");
    prepareAndActivateDraft(draftRootUrl);

    var activeRoot = selectActiveRoot(draft.getId());
    assertThat(activeRoot.getAvatarContentId()).isNotEmpty();
    assertThat(activeRoot.getAvatarStatus()).isNotEmpty();
    verifySingleCreateEvent(activeRoot.getAvatarContentId(), content);
  }

  @Test
  void createInlineAttachmentInDraftAndCancel() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());

    var content = putInlineAttachmentContent(draftRootUrl, "avatarContent");
    cancelDraft(draftRootUrl);

    waitTillExpectedHandlerMessageSize(2);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    var createContext = (AttachmentCreateEventContext) createEvents.get(0).context();
    assertThat(createContext.getData().getContent().readAllBytes())
        .isEqualTo(content.getBytes(StandardCharsets.UTF_8));

    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
    var deleteContext = (AttachmentMarkAsDeletedEventContext) deleteEvents.get(0).context();
    assertThat(deleteContext.getContentId()).isEqualTo(createContext.getContentId());
  }

  @Test
  void updateInlineAttachmentInDraftAndActivate() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());
    putInlineAttachmentContent(draftRootUrl, "originalContent");
    prepareAndActivateDraft(draftRootUrl);
    var activeRootAfterFirstActivation = selectActiveRoot(draft.getId());
    var originalContentId = activeRootAfterFirstActivation.getAvatarContentId();
    serviceHandler.clearEventContext();

    editExistingRoot(draft.getId());
    var newDraftRootUrl = getDraftRootUrl(draft.getId());
    var newContent = putInlineAttachmentContent(newDraftRootUrl, "updatedContent");
    prepareAndActivateDraft(newDraftRootUrl);

    var activeRootAfterUpdate = selectActiveRoot(draft.getId());
    assertThat(activeRootAfterUpdate.getAvatarContentId()).isNotEmpty();
    assertThat(activeRootAfterUpdate.getAvatarContentId()).isNotEqualTo(originalContentId);

    waitTillExpectedHandlerMessageSize(2);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    var createContext = (AttachmentCreateEventContext) createEvents.get(0).context();
    assertThat(createContext.getContentId()).isEqualTo(activeRootAfterUpdate.getAvatarContentId());
    assertThat(createContext.getData().getContent().readAllBytes())
        .isEqualTo(newContent.getBytes(StandardCharsets.UTF_8));

    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
    var deleteContext = (AttachmentMarkAsDeletedEventContext) deleteEvents.get(0).context();
    assertThat(deleteContext.getContentId()).isEqualTo(originalContentId);
  }

  @Test
  void deleteInlineAttachmentInDraftAndActivate() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());
    putInlineAttachmentContent(draftRootUrl, "contentToDelete");
    prepareAndActivateDraft(draftRootUrl);
    var activeRootAfterFirstActivation = selectActiveRoot(draft.getId());
    var originalContentId = activeRootAfterFirstActivation.getAvatarContentId();
    serviceHandler.clearEventContext();

    editExistingRoot(draft.getId());
    var newDraftRootUrl = getDraftRootUrl(draft.getId());
    requestHelper.executeDeleteWithMatcher(
        newDraftRootUrl + "/avatar_content", status().isNoContent());
    prepareAndActivateDraft(newDraftRootUrl);

    var activeRootAfterDelete = selectActiveRoot(draft.getId());
    assertThat(activeRootAfterDelete.getAvatarContentId()).isNull();
    verifySingleDeletionEvent(originalContentId);
  }

  @Test
  void deleteInlineAttachmentInDraftAndCancel() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());
    putInlineAttachmentContent(draftRootUrl, "contentToKeep");
    prepareAndActivateDraft(draftRootUrl);
    var activeRootAfterFirstActivation = selectActiveRoot(draft.getId());
    assertThat(activeRootAfterFirstActivation.getAvatarContentId()).isNotEmpty();
    serviceHandler.clearEventContext();

    editExistingRoot(draft.getId());
    var newDraftRootUrl = getDraftRootUrl(draft.getId());
    requestHelper.executeDeleteWithMatcher(
        newDraftRootUrl + "/avatar_content", status().isNoContent());
    cancelDraft(newDraftRootUrl);

    verifyNoAttachmentEventsCalled();
    var activeRootAfterCancel = selectActiveRoot(draft.getId());
    assertThat(activeRootAfterCancel.getAvatarContentId()).isNotEmpty();
  }

  @Test
  void contentReadableFromDraftBeforeActivation() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());
    var content = putInlineAttachmentContent(draftRootUrl, "readableContent");
    serviceHandler.clearEventContext();

    var contentUrl = draftRootUrl + "/avatar_content";
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(
            () -> {
              var response = requestHelper.executeGet(contentUrl);
              var responseContent = response.getResponse().getContentAsString();
              var matches = responseContent.equals(content);
              if (!matches) {
                logger.info(
                    "Waiting for draft content to be readable. Response: '{}', Expected: '{}'",
                    responseContent,
                    content);
              }
              return matches;
            });
    serviceHandler.clearEventContext();

    var response = requestHelper.executeGet(contentUrl);
    assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
    verifySingleReadEvent(null);
  }

  @Test
  void noChangesOnInlineAttachmentStillAvailableAfterActivate() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());
    var content = putInlineAttachmentContent(draftRootUrl, "stableContent");
    prepareAndActivateDraft(draftRootUrl);
    serviceHandler.clearEventContext();

    editExistingRoot(draft.getId());
    var newDraftRootUrl = getDraftRootUrl(draft.getId());
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        newDraftRootUrl, "{\"title\":\"changed title\"}");
    prepareAndActivateDraft(newDraftRootUrl);
    verifyNoAttachmentEventsCalled();

    var activeContentUrl = getActiveRootUrl(draft.getId()) + "/avatar_content";
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(
            () -> {
              var response = requestHelper.executeGet(activeContentUrl);
              return response.getResponse().getContentAsString().equals(content);
            });
    serviceHandler.clearEventContext();

    var response = requestHelper.executeGet(activeContentUrl);
    assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
  }

  @Test
  void errorInTransactionAfterCreateCallsDelete() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());

    testPersistenceHandler.setThrowExceptionOnUpdate(true);
    var contentUrl = draftRootUrl + "/avatar_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        contentUrl, "errorContent".getBytes(StandardCharsets.UTF_8), status().is5xxServerError());
    requestHelper.resetHelper();

    waitTillExpectedHandlerMessageSize(2);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
    var createContext = (AttachmentCreateEventContext) createEvents.get(0).context();
    var deleteContext = (AttachmentMarkAsDeletedEventContext) deleteEvents.get(0).context();
    assertThat(deleteContext.getContentId()).isEqualTo(createContext.getContentId());
  }

  @Test
  void uploadWithContentDispositionHeaderInDraftPersistsFileName() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());

    var contentUrl = draftRootUrl + "/avatar_content";
    mvc.perform(
            MockMvcRequestBuilders.put(contentUrl)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"draft-file.png\"")
                .content("draft-content".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isNoContent());

    prepareAndActivateDraft(draftRootUrl);

    var activeRoot = selectActiveRoot(draft.getId());
    assertThat(activeRoot.getAvatarContentId()).isNotEmpty();
    assertThat(activeRoot.getAvatarFileName()).isEqualTo("draft-file.png");
  }

  @Test
  void multiEntityIsolation_activatingOneEntityDoesNotAffectOther() throws Exception {
    var draftA = createNewDraft();
    var draftAUrl = getDraftRootUrl(draftA.getId());
    putInlineAttachmentContent(draftAUrl, "contentA");

    var draftB = createNewDraft();
    var draftBUrl = getDraftRootUrl(draftB.getId());
    putInlineAttachmentContent(draftBUrl, "contentB");

    prepareAndActivateDraft(draftAUrl);

    var activeRootA = selectActiveRoot(draftA.getId());
    assertThat(activeRootA.getAvatarContentId()).isNotEmpty();

    prepareAndActivateDraft(draftBUrl);

    var activeRootB = selectActiveRoot(draftB.getId());
    assertThat(activeRootB.getAvatarContentId()).isNotEmpty();
    assertThat(activeRootB.getAvatarContentId()).isNotEqualTo(activeRootA.getAvatarContentId());

    var contentBUrl = getActiveRootUrl(draftB.getId()) + "/avatar_content";
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(
            () -> {
              var response = requestHelper.executeGet(contentBUrl);
              return response.getResponse().getContentAsString().equals("contentB");
            });

    var response = requestHelper.executeGet(contentBUrl);
    assertThat(response.getResponse().getContentAsString()).isEqualTo("contentB");
  }

  @Test
  void putOversizedContentToCoverImageInDraftReturnsError() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());

    var url = draftRootUrl + "/coverImage_content";
    byte[] oversizedContent = new byte[6 * 1024 * 1024]; // 6MB > 5MB limit
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, oversizedContent, status().is4xxClientError());
  }

  @Test
  void updateInlineAttachmentInDraftAndCancelDeletesNewContent() throws Exception {
    var draft = createNewDraft();
    var draftRootUrl = getDraftRootUrl(draft.getId());
    putInlineAttachmentContent(draftRootUrl, "originalContent");
    prepareAndActivateDraft(draftRootUrl);
    var activeRootAfterFirstActivation = selectActiveRoot(draft.getId());
    var originalContentId = activeRootAfterFirstActivation.getAvatarContentId();
    assertThat(originalContentId).isNotEmpty();
    serviceHandler.clearEventContext();

    editExistingRoot(draft.getId());
    var newDraftRootUrl = getDraftRootUrl(draft.getId());
    putInlineAttachmentContent(newDraftRootUrl, "updatedContent");
    cancelDraft(newDraftRootUrl);

    waitTillExpectedHandlerMessageSize(2);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
    var createContext = (AttachmentCreateEventContext) createEvents.get(0).context();
    var deleteContext = (AttachmentMarkAsDeletedEventContext) deleteEvents.get(0).context();
    assertThat(deleteContext.getContentId()).isEqualTo(createContext.getContentId());

    var activeRootAfterCancel = selectActiveRoot(draft.getId());
    assertThat(activeRootAfterCancel.getAvatarContentId()).isEqualTo(originalContentId);
  }

  // Helper methods

  private DraftRoots createNewDraft() throws Exception {
    var responseData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(
            BASE_URL + "DraftRoots", "{}");
    return Struct.access(responseData).as(DraftRoots.class);
  }

  private String getDraftRootUrl(String rootId) {
    return BASE_URL + "DraftRoots(ID=" + rootId + ",IsActiveEntity=false)";
  }

  private String getActiveRootUrl(String rootId) {
    return BASE_URL + "DraftRoots(ID=" + rootId + ",IsActiveEntity=true)";
  }

  private void prepareAndActivateDraft(String draftRootUrl) throws Exception {
    var draftPrepareUrl = draftRootUrl + "/TestDraftService.draftPrepare";
    var draftActivateUrl = draftRootUrl + "/TestDraftService.draftActivate";
    requestHelper.executePostWithMatcher(
        draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());
    requestHelper.executePostWithMatcher(draftActivateUrl, "{}", status().isOk());
  }

  private void editExistingRoot(String rootId) throws Exception {
    var url = getActiveRootUrl(rootId) + "/TestDraftService.draftEdit";
    requestHelper.executePostWithMatcher(url, "{\"PreserveChanges\":true}", status().isOk());
  }

  private void cancelDraft(String draftRootUrl) throws Exception {
    requestHelper.executeDeleteWithMatcher(draftRootUrl, status().isNoContent());
  }

  private String putInlineAttachmentContent(String draftRootUrl, String content) throws Exception {
    var contentUrl = draftRootUrl + "/avatar_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        contentUrl, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
    return content;
  }

  private DraftRoots selectActiveRoot(String rootId) {
    var select =
        Select.from(DraftRoots_.CDS_NAME)
            .where(root -> root.get(DraftRoots.ID).eq(rootId))
            .columns(StructuredType::_all);
    return persistenceService.run(select).single(DraftRoots.class);
  }

  private void verifySingleCreateEvent(String contentId, String content) {
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_READ_ATTACHMENT,
        AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents)
        .hasSize(1)
        .first()
        .satisfies(
            event -> {
              assertThat(event.context()).isInstanceOf(AttachmentCreateEventContext.class);
              var createContext = (AttachmentCreateEventContext) event.context();
              assertThat(createContext.getContentId()).isEqualTo(contentId);
              assertThat(createContext.getData().getContent().readAllBytes())
                  .isEqualTo(content.getBytes(StandardCharsets.UTF_8));
            });
  }

  private void verifySingleDeletionEvent(String contentId) {
    waitTillExpectedHandlerMessageSize(1);
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_CREATE_ATTACHMENT, AttachmentService.EVENT_READ_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents)
        .hasSize(1)
        .first()
        .satisfies(
            event -> {
              assertThat(event.context()).isInstanceOf(AttachmentMarkAsDeletedEventContext.class);
              var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
              assertThat(deleteContext.getContentId()).isEqualTo(contentId);
              assertThat(deleteContext.getDeletionUserInfo().getName()).isEqualTo("anonymous");
              assertThat(deleteContext.getDeletionUserInfo().getIsSystemUser()).isFalse();
            });
  }

  private void verifySingleReadEvent(String contentId) {
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_CREATE_ATTACHMENT,
        AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    var readContext = serviceHandler.getEventContext();
    assertThat(readContext)
        .hasSize(1)
        .first()
        .satisfies(
            event -> {
              assertThat(event.event()).isEqualTo(AttachmentService.EVENT_READ_ATTACHMENT);
              if (contentId != null) {
                assertThat(((AttachmentReadEventContext) event.context()).getContentId())
                    .isEqualTo(contentId);
              }
            });
  }

  private void verifyNoAttachmentEventsCalled() {
    assertThat(serviceHandler.getEventContext()).isEmpty();
  }

  private void verifyEventContextEmptyForEvent(String... events) {
    Arrays.stream(events)
        .forEach(event -> assertThat(serviceHandler.getEventContextForEvent(event)).isEmpty());
  }

  private void waitTillExpectedHandlerMessageSize(int expectedSize) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(
            () -> {
              var eventCalls = serviceHandler.getEventContext().size();
              logger.debug(
                  "Waiting for expected size '{}' in handler context, was '{}'",
                  expectedSize,
                  eventCalls);
              return eventCalls >= expectedSize;
            });
  }
}

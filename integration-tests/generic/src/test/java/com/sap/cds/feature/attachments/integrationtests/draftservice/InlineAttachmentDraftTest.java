/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftInlineOnly;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftInlineOnly_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.services.persistence.PersistenceService;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class InlineAttachmentDraftTest {

  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
  private static final String DRAFT_INLINE_URL = BASE_URL + "DraftInlineOnly";

  @Autowired private TestPluginAttachmentsServiceHandler serviceHandler;
  @Autowired private MockHttpRequestHelper requestHelper;
  @Autowired private PersistenceService persistenceService;
  @Autowired private TableDataDeleter dataDeleter;

  @AfterEach
  void teardown() {
    dataDeleter.deleteData(
        DraftInlineOnly_.CDS_NAME,
        DraftInlineOnly_.CDS_NAME + "_drafts",
        DraftRoots_.CDS_NAME,
        DraftRoots_.CDS_NAME + "_drafts",
        "cds.outbox.Messages");
    serviceHandler.clearEventContext();
    serviceHandler.clearDocuments();
    requestHelper.resetHelper();
  }

  @Test
  void draftCreatePatchAndActivateWithInlineAttachment() throws Exception {
    var draft = createNewDraft();
    var draftUrl = getDraftUrl(draft.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        draftUrl, "{\"title\":\"draft inline\"}");

    putDraftAvatarContent(draft.getId(), "draft avatar content");
    prepareAndActivate(draftUrl);

    var active = selectActiveInlineOnly(draft);
    assertThat(active.getTitle()).isEqualTo("draft inline");
    assertThat(active.getAvatarContentId()).isNotNull();

    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    var context = (AttachmentCreateEventContext) createEvents.get(0).context();
    assertThat(context.getData().getContent().readAllBytes())
        .isEqualTo("draft avatar content".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void draftCancelWithInlineAttachment() throws Exception {
    var draft = createNewDraft();
    var draftUrl = getDraftUrl(draft.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        draftUrl, "{\"title\":\"cancel me\"}");

    putDraftAvatarContent(draft.getId(), "cancel this content");

    awaitNumberOfExpectedEvents(1);
    serviceHandler.clearEventContext();

    cancelDraft(draftUrl);

    awaitNumberOfExpectedEvents(1);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
  }

  @Test
  void draftEditUpdateInlineAndActivate() throws Exception {
    var draft = createNewDraft();
    var draftUrl = getDraftUrl(draft.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        draftUrl, "{\"title\":\"edit test\"}");
    putDraftAvatarContent(draft.getId(), "original content");
    prepareAndActivate(draftUrl);
    serviceHandler.clearEventContext();

    editExistingDraft(draft.getId());
    putDraftAvatarContent(draft.getId(), "updated content");
    prepareAndActivate(getDraftUrl(draft.getId(), false));

    awaitNumberOfExpectedEvents(2);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(createEvents).hasSize(1);
    assertThat(deleteEvents).hasSize(1);
  }

  @Test
  void draftDeleteActiveEntityWithInlineAttachment() throws Exception {
    var draft = createNewDraft();
    var draftUrl = getDraftUrl(draft.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        draftUrl, "{\"title\":\"to delete\"}");
    putDraftAvatarContent(draft.getId(), "delete this");
    prepareAndActivate(draftUrl);
    serviceHandler.clearEventContext();

    var activeUrl = getDraftUrl(draft.getId(), true);
    requestHelper.executeDeleteWithMatcher(activeUrl, status().isNoContent());

    awaitNumberOfExpectedEvents(1);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
  }

  @Test
  void readInlineAttachmentContentFromActiveDraft() throws Exception {
    var draft = createNewDraft();
    var draftUrl = getDraftUrl(draft.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        draftUrl, "{\"title\":\"read active\"}");
    putDraftAvatarContent(draft.getId(), "readable");
    prepareAndActivate(draftUrl);
    serviceHandler.clearEventContext();

    var contentUrl =
        DRAFT_INLINE_URL + "(ID=" + draft.getId() + ",IsActiveEntity=true)/avatar_content";
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(
            () -> {
              var response = requestHelper.executeGet(contentUrl);
              return response.getResponse().getContentAsString().equals("readable");
            });
    serviceHandler.clearEventContext();

    var response = requestHelper.executeGet(contentUrl);
    assertThat(response.getResponse().getContentAsString()).isEqualTo("readable");
    var readEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
    assertThat(readEvents).hasSize(1);
  }

  @Test
  void draftRootsWithBothInlineAndCompositionAttachments() throws Exception {
    var responseRoot = createNewDraftRoot();
    var rootUrl = getDraftRootUrl(responseRoot.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        rootUrl, "{\"title\":\"both kinds\"}");

    putDraftRootAvatarContent(responseRoot.getId(), "draft root avatar");

    var item = createDraftItem(rootUrl);
    createDraftAttachment(item.getId(), "comp content");

    prepareAndActivate(rootUrl);

    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(2);

    var active = selectActiveDraftRoot(responseRoot);
    assertThat(active.getAvatarContentId()).isNotNull();
    assertThat(active.getItems().get(0).getAttachments().get(0).getContentId()).isNotNull();
  }

  private DraftInlineOnly createNewDraft() throws Exception {
    var cdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(DRAFT_INLINE_URL, "{}");
    return Struct.access(cdsData).as(DraftInlineOnly.class);
  }

  private void editExistingDraft(String id) throws Exception {
    var url = getDraftUrl(id, true) + "/TestDraftService.draftEdit";
    requestHelper.executePostWithODataResponseAndAssertStatus(
        url, "{\"PreserveChanges\":true}", HttpStatus.OK);
  }

  private String getDraftUrl(String id, boolean isActiveEntity) {
    return DRAFT_INLINE_URL + "(ID=" + id + ",IsActiveEntity=" + isActiveEntity + ")";
  }

  private void putDraftAvatarContent(String id, String content) throws Exception {
    var url = DRAFT_INLINE_URL + "(ID=" + id + ",IsActiveEntity=false)/avatar_content";
    requestHelper.setContentType("application/octet-stream");
    requestHelper.executePutWithMatcher(
        url, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
  }

  private void prepareAndActivate(String entityUrl) throws Exception {
    var prepareUrl = entityUrl + "/TestDraftService.draftPrepare";
    var activateUrl = entityUrl + "/TestDraftService.draftActivate";
    requestHelper.executePostWithMatcher(
        prepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());
    requestHelper.executePostWithMatcher(activateUrl, "{}", status().isOk());
  }

  private void cancelDraft(String entityUrl) throws Exception {
    requestHelper.executeDeleteWithMatcher(entityUrl, status().isNoContent());
  }

  private DraftInlineOnly selectActiveInlineOnly(DraftInlineOnly draft) {
    var select =
        Select.from(DraftInlineOnly_.CDS_NAME)
            .where(e -> e.get(DraftInlineOnly.ID).eq(draft.getId()))
            .columns(StructuredType::_all);
    return persistenceService.run(select).single(DraftInlineOnly.class);
  }

  private DraftRoots createNewDraftRoot() throws Exception {
    var url = BASE_URL + "DraftRoots";
    var cdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(url, "{}");
    return Struct.access(cdsData).as(DraftRoots.class);
  }

  private String getDraftRootUrl(String id, boolean isActiveEntity) {
    return BASE_URL + "DraftRoots(ID=" + id + ",IsActiveEntity=" + isActiveEntity + ")";
  }

  private void putDraftRootAvatarContent(String rootId, String content) throws Exception {
    var url = BASE_URL + "DraftRoots(ID=" + rootId + ",IsActiveEntity=false)/avatar_content";
    requestHelper.setContentType("application/octet-stream");
    requestHelper.executePutWithMatcher(
        url, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
  }

  private com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.Items
      createDraftItem(String rootUrl) throws Exception {
    var item =
        com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.Items
            .create();
    item.setTitle("draft item");
    var itemUrl = rootUrl + "/items";
    var cdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(itemUrl, item.toJson());
    return Struct.access(cdsData)
        .as(
            com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.Items
                .class);
  }

  private void createDraftAttachment(String itemId, String content) throws Exception {
    var attachment =
        com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments
            .create();
    attachment.setFileName("draftAttachment.txt");
    var postUrl = BASE_URL + "Items(ID=" + itemId + ",IsActiveEntity=false)/attachments";
    var cdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(
            postUrl, attachment.toJson());
    var created =
        Struct.access(cdsData)
            .as(
                com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments
                    .Attachments.class);
    var putUrl =
        BASE_URL
            + "Items_attachments(up__ID="
            + itemId
            + ",ID="
            + created.getId()
            + ",IsActiveEntity=false)/content";
    requestHelper.setContentType("text/plain");
    requestHelper.executePutWithMatcher(
        putUrl, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
  }

  private DraftRoots selectActiveDraftRoot(DraftRoots draft) {
    var select =
        Select.from(DraftRoots_.CDS_NAME)
            .where(r -> r.get(DraftRoots.ID).eq(draft.getId()))
            .columns(
                StructuredType::_all,
                r ->
                    r.to(DraftRoots.ITEMS)
                        .expand(
                            StructuredType::_all,
                            i ->
                                i.to(
                                        com.sap.cds.feature.attachments.generated.integration.test
                                            .cds4j.testdraftservice.Items.ATTACHMENTS)
                                    .expand(),
                            i ->
                                i.to(
                                        com.sap.cds.feature.attachments.generated.integration.test
                                            .cds4j.testdraftservice.Items.ATTACHMENT_ENTITIES)
                                    .expand()));
    return persistenceService.run(select).single(DraftRoots.class);
  }

  private void awaitNumberOfExpectedEvents(int expectedSize) {
    Awaitility.await()
        .atMost(60, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .pollInterval(2, TimeUnit.SECONDS)
        .until(() -> serviceHandler.getEventContext().size() >= expectedSize);
  }
}

/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.InlineOnlyTable;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.InlineOnlyTable_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.ItemEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.RootEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.services.persistence.PersistenceService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class InlineAttachmentNonDraftTest {

  private static final String INLINE_ONLY_URL =
      MockHttpRequestHelper.ODATA_BASE_URL + "TestService/InlineOnlyTable";
  private static final String ROOTS_URL =
      MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots";

  @Autowired private TestPluginAttachmentsServiceHandler serviceHandler;
  @Autowired private MockHttpRequestHelper requestHelper;
  @Autowired private PersistenceService persistenceService;
  @Autowired private TableDataDeleter dataDeleter;

  @AfterEach
  void teardown() {
    dataDeleter.deleteData(InlineOnlyTable_.CDS_NAME, Roots_.CDS_NAME);
    serviceHandler.clearEventContext();
    serviceHandler.clearDocuments();
    requestHelper.resetHelper();
  }

  @Test
  void createEntityWithInlineAttachmentContent() throws Exception {
    var entity = InlineOnlyTable.create();
    entity.setTitle("inline test");
    postInlineOnly(entity);

    var created = selectSingleInlineOnly();
    assertThat(created.getTitle()).isEqualTo("inline test");
    assertThat(created.getAvatarContentId()).isNull();
    assertThat(serviceHandler.getEventContext()).isEmpty();

    putAvatarContent(created.getId(), "avatar content");
    var afterPut = selectSingleInlineOnly();

    assertThat(afterPut.getAvatarContentId()).isNotNull();
    verifySingleCreateEvent("avatar content");
  }

  @Test
  void readEntityWithInlineAttachment() throws Exception {
    var entity = InlineOnlyTable.create();
    entity.setTitle("read test");
    postInlineOnly(entity);
    var created = selectSingleInlineOnly();
    putAvatarContent(created.getId(), "readable content");
    serviceHandler.clearEventContext();

    var url = INLINE_ONLY_URL + "(" + created.getId() + ")";
    var response =
        requestHelper.executeGetWithSingleODataResponseAndAssertStatus(
            url, InlineOnlyTable.class, HttpStatus.OK);

    assertThat(response.getAvatarContentId()).isNotNull();
    assertThat(response.get("avatar_content@mediaContentType")).isNotNull();
  }

  @Test
  void readInlineAttachmentContentViaNavigationUrl() throws Exception {
    var entity = InlineOnlyTable.create();
    entity.setTitle("nav read test");
    postInlineOnly(entity);
    var created = selectSingleInlineOnly();
    putAvatarContent(created.getId(), "nav content");
    serviceHandler.clearEventContext();

    var url = INLINE_ONLY_URL + "(" + created.getId() + ")/avatar_content";
    var response = requestHelper.executeGet(url);

    assertThat(response.getResponse().getContentAsString()).isEqualTo("nav content");
    var readEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
    assertThat(readEvents).hasSize(1);
  }

  @Test
  void updateInlineAttachment() throws Exception {
    var entity = InlineOnlyTable.create();
    entity.setTitle("update test");
    postInlineOnly(entity);
    var created = selectSingleInlineOnly();
    putAvatarContent(created.getId(), "original content");
    serviceHandler.clearEventContext();

    putAvatarContent(created.getId(), "updated content");

    awaitNumberOfExpectedEvents(2);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(createEvents).hasSize(1);
    assertThat(deleteEvents).hasSize(1);

    var createContext = (AttachmentCreateEventContext) createEvents.get(0).context();
    assertThat(createContext.getData().getContent().readAllBytes())
        .isEqualTo("updated content".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void deleteEntityWithInlineAttachment() throws Exception {
    var entity = InlineOnlyTable.create();
    entity.setTitle("delete test");
    postInlineOnly(entity);
    var created = selectSingleInlineOnly();
    putAvatarContent(created.getId(), "delete me");
    var afterPut = selectSingleInlineOnly();
    serviceHandler.clearEventContext();

    var url = INLINE_ONLY_URL + "(" + afterPut.getId() + ")";
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    awaitNumberOfExpectedEvents(1);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
    var deleteContext = (AttachmentMarkAsDeletedEventContext) deleteEvents.get(0).context();
    assertThat(deleteContext.getContentId()).isEqualTo(afterPut.getAvatarContentId());
  }

  @Test
  void entityWithBothInlineAndCompositionAttachments() throws Exception {
    var root =
        RootEntityBuilder.create()
            .setTitle("both kinds")
            .addAttachments(
                AttachmentsEntityBuilder.create()
                    .setFileName("composed.txt")
                    .setMimeType("text/plain"))
            .addItems(
                ItemEntityBuilder.create()
                    .setTitle("item1")
                    .addAttachmentEntities(
                        AttachmentsEntityBuilder.create()
                            .setFileName("itemFile.txt")
                            .setMimeType("text/plain")))
            .build();
    requestHelper.executePostWithMatcher(ROOTS_URL, root.toJson(), status().isCreated());

    var selectedRoot = selectStoredRoot();
    assertThat(selectedRoot.getAttachments()).hasSize(1);
    assertThat(selectedRoot.getAvatarContentId()).isNull();
    assertThat(serviceHandler.getEventContext()).isEmpty();

    putRootAvatarContent(selectedRoot.getId(), "root avatar content");
    verifySingleCreateEvent("root avatar content");
    serviceHandler.clearEventContext();

    putCompositionAttachmentContent(selectedRoot);
    verifySingleCreateEvent(null);
    serviceHandler.clearEventContext();

    var url = ROOTS_URL + "(" + selectedRoot.getId() + ")";
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    awaitNumberOfExpectedEvents(2);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(2);
  }

  private void postInlineOnly(InlineOnlyTable entity) throws Exception {
    requestHelper.executePostWithMatcher(INLINE_ONLY_URL, entity.toJson(), status().isCreated());
  }

  private InlineOnlyTable selectSingleInlineOnly() {
    var select = Select.from(InlineOnlyTable_.class).columns(StructuredType::_all);
    return persistenceService.run(select).single(InlineOnlyTable.class);
  }

  private void putAvatarContent(String entityId, String content) throws Exception {
    var url = INLINE_ONLY_URL + "(" + entityId + ")/avatar_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        url, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
  }

  private void putRootAvatarContent(String rootId, String content) throws Exception {
    var url = ROOTS_URL + "(" + rootId + ")/avatar_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        url, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
  }

  private void putCompositionAttachmentContent(Roots root) throws Exception {
    var attachment = root.getAttachments().get(0);
    var url =
        MockHttpRequestHelper.ODATA_BASE_URL
            + "TestService/AttachmentEntity("
            + attachment.getId()
            + ")/content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        url, "composed content".getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();
  }

  private Roots selectStoredRoot() {
    var select =
        Select.from(Roots_.class)
            .columns(
                StructuredType::_all,
                r -> r.attachments().expand(),
                r ->
                    r.items()
                        .expand(
                            StructuredType::_all,
                            i -> i.attachments().expand(),
                            i -> i.attachmentEntities().expand()));
    return persistenceService.run(select).single(Roots.class);
  }

  private void verifySingleCreateEvent(String expectedContent) throws IOException {
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    if (expectedContent != null) {
      var context = (AttachmentCreateEventContext) createEvents.get(0).context();
      assertThat(context.getData().getContent().readAllBytes())
          .isEqualTo(expectedContent.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void awaitNumberOfExpectedEvents(int expectedSize) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> serviceHandler.getEventContext().size() >= expectedSize);
  }
}

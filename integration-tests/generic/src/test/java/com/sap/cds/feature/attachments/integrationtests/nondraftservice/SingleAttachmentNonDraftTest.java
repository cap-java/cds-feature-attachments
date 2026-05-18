/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.EventContextHolder;
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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class SingleAttachmentNonDraftTest {

  @Autowired private TestPluginAttachmentsServiceHandler serviceHandler;
  @Autowired private MockHttpRequestHelper requestHelper;
  @Autowired private PersistenceService persistenceService;
  @Autowired private TableDataDeleter dataDeleter;
  @Autowired private TestPersistenceHandler testPersistenceHandler;
  @Autowired private MockMvc mvc;

  @AfterEach
  void teardown() {
    dataDeleter.deleteData(Roots_.CDS_NAME);
    serviceHandler.clearEventContext();
    serviceHandler.clearDocuments();
    requestHelper.resetHelper();
    testPersistenceHandler.reset();
  }

  @Test
  void createRootWithoutInlineAttachmentWorks() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);

    var selectedRoot = selectStoredRoot();
    assertThat(selectedRoot.getId()).isNotEmpty();
    assertThat(selectedRoot.getTitle()).isEqualTo(root.getTitle());
    assertThat(selectedRoot.getAvatarContent()).isNull();
    assertThat(selectedRoot.getAvatarContentId()).isNull();
    assertThat(selectedRoot.getAvatarFileName()).isNull();
    verifyNoAttachmentEventsCalled();
  }

  @Test
  void putContentToInlineAttachmentOnRootWorks() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var content = putInlineAttachmentContentOnRoot(selectedRoot.getId());
    var rootAfterPut = selectStoredRoot();

    assertThat(rootAfterPut.getAvatarContentId()).isNotEmpty();
    assertThat(rootAfterPut.getAvatarStatus()).isNotEmpty();
    verifySingleCreateEvent(rootAfterPut.getAvatarContentId(), content);
  }

  @Test
  void readInlineAttachmentContentOnRootReturnsContent() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var content = putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();

    var url = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    var response = requestHelper.executeGet(url);

    assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
    verifySingleReadEvent(rootAfterPut.getAvatarContentId());
  }

  @Test
  void readInlineAttachmentContentReturnsCorrectContentTypeHeader() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var content = putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();

    var url = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    var response = requestHelper.executeGet(url);

    assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
    assertThat(response.getResponse().getContentType()).startsWith("application/octet-stream");
  }

  @Test
  void readInlineAttachmentContentReturnsContentDispositionHeader() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();

    var url = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    var response = requestHelper.executeGet(url);

    var contentDisposition = response.getResponse().getHeader("Content-Disposition");
    assertThat(contentDisposition).isNotNull();
    assertThat(contentDisposition).startsWith("inline");
  }

  @Test
  void readInlineAttachmentContentReturnsFilenameInContentDisposition() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    requestHelper.resetHelper(); // Reset after PUT to use JSON for PATCH
    var rootAfterPut = selectStoredRoot();

    var patchUrl = buildRootUrl(rootAfterPut.getId());
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        patchUrl, "{\"avatar_fileName\": \"test-file.bin\"}");

    var url = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    var response = requestHelper.executeGet(url);

    var contentDisposition = response.getResponse().getHeader("Content-Disposition");
    assertThat(contentDisposition).isNotNull();
    assertThat(contentDisposition).contains("filename=\"test-file.bin\"");
  }

  @Test
  void readInlineAttachmentContentWithCustomMimeTypeReturnsCorrectContentType() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var url = buildRootUrl(selectedRoot.getId()) + "/avatar_content";
    requestHelper.setContentType(MediaType.IMAGE_PNG);
    requestHelper.executePutWithMatcher(
        url, "fake-image-content".getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();

    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();

    var readUrl = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    var response = requestHelper.executeGet(readUrl);

    assertThat(response.getResponse().getContentType()).startsWith("image/png");
  }

  @Test
  void selectInlineAttachmentIncludesMediaContentTypeAnnotation() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var url = buildRootUrl(selectedRoot.getId()) + "/avatar_content";
    requestHelper.setContentType(MediaType.TEXT_PLAIN);
    requestHelper.executePutWithMatcher(
        url, "test-content".getBytes(StandardCharsets.UTF_8), status().isNoContent());
    requestHelper.resetHelper();

    serviceHandler.clearEventContext();

    var selectUrl =
        MockHttpRequestHelper.ODATA_BASE_URL
            + "TestService/Roots("
            + selectedRoot.getId()
            + ")?$select=avatar_content,avatar_mimeType";
    var response =
        requestHelper.executeGetWithSingleODataResponseAndAssertStatus(selectUrl, HttpStatus.OK);

    assertThat(response).contains("avatar_content@mediaContentType");
    assertThat(response).contains("text/plain");
  }

  @Test
  void deleteInlineAttachmentContentOnRootClearsContent() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();
    var contentIdBeforeDelete = rootAfterPut.getAvatarContentId();

    var url = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    requestHelper.executeDelete(url);

    var rootAfterDelete = selectStoredRoot();
    assertThat(rootAfterDelete.getAvatarContentId()).isNull();
    assertThat(rootAfterDelete.getAvatarContent()).isNull();
    verifySingleDeletionEvent(contentIdBeforeDelete);
  }

  @Test
  void updateInlineAttachmentContentOnRootWorks() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterFirstPut = selectStoredRoot();
    var firstContentId = rootAfterFirstPut.getAvatarContentId();

    var newContent = putInlineAttachmentContentOnRoot(rootAfterFirstPut.getId(), "newContent");
    var rootAfterSecondPut = selectStoredRoot();

    assertThat(rootAfterSecondPut.getAvatarContentId()).isNotEmpty();
    assertThat(rootAfterSecondPut.getAvatarContentId()).isNotEqualTo(firstContentId);
    verifySingleCreateAndDeleteEvent(
        rootAfterSecondPut.getAvatarContentId(), firstContentId, newContent);
  }

  @Test
  void deleteRootDeletesInlineAttachmentContent() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();
    var contentId = rootAfterPut.getAvatarContentId();

    var url = buildRootUrl(rootAfterPut.getId());
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    verifySingleDeletionEvent(contentId);
  }

  @Test
  void inlineAttachmentReadViaExpandHasNoFilledContent() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();
    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();

    var url = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots?$select=ID,avatar_content";
    var response =
        requestHelper.executeGetWithSingleODataResponseAndAssertStatus(
            url, Roots.class, HttpStatus.OK);

    assertThat(response.getAvatarContent()).isNull();
    verifyNoAttachmentEventsCalled();
  }

  @Test
  void createRootWithItemWithoutInlineAttachmentWorks() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);

    var selectedRoot = selectStoredRootWithItems();
    assertThat(selectedRoot.getItems()).hasSize(1);
    var item = selectedRoot.getItems().get(0);
    assertThat(item.getIconContent()).isNull();
    assertThat(item.getIconContentId()).isNull();
    verifyNoAttachmentEventsCalled();
  }

  @Test
  void putContentToInlineAttachmentOnItemWorks() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    var content = putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    var rootAfterPut = selectStoredRootWithItems();
    var itemAfterPut = rootAfterPut.getItems().get(0);

    assertThat(itemAfterPut.getIconContentId()).isNotEmpty();
    assertThat(itemAfterPut.getIconStatus()).isNotEmpty();
    verifySingleCreateEvent(itemAfterPut.getIconContentId(), content);
  }

  @Test
  void readInlineAttachmentContentOnItemReturnsContent() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    var content = putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRootWithItems();
    var itemAfterPut = rootAfterPut.getItems().get(0);

    var url = buildItemUrl(selectedRoot.getId(), item.getId()) + "/icon_content";
    var response = requestHelper.executeGet(url);

    assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
    verifySingleReadEvent(itemAfterPut.getIconContentId());
  }

  @Test
  void deleteInlineAttachmentContentOnItemClearsContent() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRootWithItems();
    var itemAfterPut = rootAfterPut.getItems().get(0);
    var contentIdBeforeDelete = itemAfterPut.getIconContentId();

    var url = buildItemUrl(selectedRoot.getId(), item.getId()) + "/icon_content";
    requestHelper.executeDelete(url);

    var rootAfterDelete = selectStoredRootWithItems();
    var itemAfterDelete = rootAfterDelete.getItems().get(0);
    assertThat(itemAfterDelete.getIconContentId()).isNull();
    assertThat(itemAfterDelete.getIconContent()).isNull();
    verifySingleDeletionEvent(contentIdBeforeDelete);
  }

  @Test
  void updateInlineAttachmentContentOnItemWorks() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    serviceHandler.clearEventContext();
    var rootAfterFirstPut = selectStoredRootWithItems();
    var itemAfterFirstPut = rootAfterFirstPut.getItems().get(0);
    var firstContentId = itemAfterFirstPut.getIconContentId();

    var newContent =
        putInlineAttachmentContentOnItem(
            rootAfterFirstPut.getId(), itemAfterFirstPut.getId(), "newContent");
    var rootAfterSecondPut = selectStoredRootWithItems();
    var itemAfterSecondPut = rootAfterSecondPut.getItems().get(0);

    assertThat(itemAfterSecondPut.getIconContentId()).isNotEmpty();
    assertThat(itemAfterSecondPut.getIconContentId()).isNotEqualTo(firstContentId);
    verifySingleCreateAndDeleteEvent(
        itemAfterSecondPut.getIconContentId(), firstContentId, newContent);
  }

  @Test
  void deleteItemDeletesInlineAttachmentContent() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRootWithItems();
    var itemAfterPut = rootAfterPut.getItems().get(0);
    var contentId = itemAfterPut.getIconContentId();

    var url = buildItemUrl(selectedRoot.getId(), item.getId());
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    verifySingleDeletionEvent(contentId);
  }

  @Test
  void deleteRootDeletesInlineAttachmentOnItemContent() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRootWithItems();
    var itemAfterPut = rootAfterPut.getItems().get(0);
    var contentId = itemAfterPut.getIconContentId();

    var url = buildRootUrl(rootAfterPut.getId());
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    verifySingleDeletionEvent(contentId);
  }

  @Test
  void deleteRootDeletesBothRootAndItemInlineAttachments() throws Exception {
    var root = buildRootWithItem();
    postServiceRoot(root);
    var selectedRoot = selectStoredRootWithItems();
    var item = selectedRoot.getItems().get(0);

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    putInlineAttachmentContentOnItem(selectedRoot.getId(), item.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRootWithItems();
    var itemAfterPut = rootAfterPut.getItems().get(0);
    var rootContentId = rootAfterPut.getAvatarContentId();
    var itemContentId = itemAfterPut.getIconContentId();

    var url = buildRootUrl(rootAfterPut.getId());
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    verifyTwoDeletionEvents(rootContentId, itemContentId);
  }

  @Test
  void twoInlineAttachmentsOnSameEntityDoNotCollide() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId(), "avatarData");
    serviceHandler.clearEventContext();
    var coverImageContent = putCoverImageContentOnRoot(selectedRoot.getId(), "coverImageData");

    var rootAfterPut = selectStoredRoot();

    assertThat(rootAfterPut.getAvatarContentId()).isNotEmpty();
    assertThat(rootAfterPut.getCoverImageContentId()).isNotEmpty();
    assertThat(rootAfterPut.getAvatarContentId())
        .isNotEqualTo(rootAfterPut.getCoverImageContentId());

    verifySingleCreateEvent(rootAfterPut.getCoverImageContentId(), coverImageContent);
  }

  @Test
  void readingOneInlineAttachmentDoesNotAffectOther() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var avatarContent = putInlineAttachmentContentOnRoot(selectedRoot.getId(), "avatarData");
    var coverImageContent = putCoverImageContentOnRoot(selectedRoot.getId(), "coverImageData");
    serviceHandler.clearEventContext();

    var rootAfterPut = selectStoredRoot();

    var avatarUrl = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    var avatarResponse = requestHelper.executeGet(avatarUrl);
    assertThat(avatarResponse.getResponse().getContentAsString()).isEqualTo(avatarContent);

    verifySingleReadEvent(rootAfterPut.getAvatarContentId());
    serviceHandler.clearEventContext();

    var coverImageUrl = buildRootUrl(rootAfterPut.getId()) + "/coverImage_content";
    var coverImageResponse = requestHelper.executeGet(coverImageUrl);
    assertThat(coverImageResponse.getResponse().getContentAsString()).isEqualTo(coverImageContent);

    verifySingleReadEvent(rootAfterPut.getCoverImageContentId());
  }

  @Test
  void deletingOneInlineAttachmentDoesNotAffectOther() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId(), "avatarData");
    putCoverImageContentOnRoot(selectedRoot.getId(), "coverImageData");
    serviceHandler.clearEventContext();

    var rootAfterPut = selectStoredRoot();
    var avatarContentId = rootAfterPut.getAvatarContentId();
    var coverImageContentId = rootAfterPut.getCoverImageContentId();

    var avatarUrl = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    requestHelper.executeDelete(avatarUrl);

    var rootAfterDelete = selectStoredRoot();

    assertThat(rootAfterDelete.getAvatarContentId()).isNull();
    assertThat(rootAfterDelete.getAvatarContent()).isNull();

    assertThat(rootAfterDelete.getCoverImageContentId()).isEqualTo(coverImageContentId);

    verifySingleDeletionEvent(avatarContentId);
  }

  @Test
  void updatingOneInlineAttachmentDoesNotAffectOther() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId(), "avatarData");
    putCoverImageContentOnRoot(selectedRoot.getId(), "coverImageData");
    serviceHandler.clearEventContext();

    var rootAfterFirstPut = selectStoredRoot();
    var originalAvatarContentId = rootAfterFirstPut.getAvatarContentId();
    var originalCoverImageContentId = rootAfterFirstPut.getCoverImageContentId();

    var newAvatarContent =
        putInlineAttachmentContentOnRoot(rootAfterFirstPut.getId(), "newAvatarData");

    var rootAfterUpdate = selectStoredRoot();

    assertThat(rootAfterUpdate.getAvatarContentId()).isNotEmpty();
    assertThat(rootAfterUpdate.getAvatarContentId()).isNotEqualTo(originalAvatarContentId);

    assertThat(rootAfterUpdate.getCoverImageContentId()).isEqualTo(originalCoverImageContentId);

    verifySingleCreateAndDeleteEvent(
        rootAfterUpdate.getAvatarContentId(), originalAvatarContentId, newAvatarContent);
  }

  @Test
  void deleteRootDeletesBothInlineAttachments() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId(), "avatarData");
    putCoverImageContentOnRoot(selectedRoot.getId(), "coverImageData");
    serviceHandler.clearEventContext();

    var rootAfterPut = selectStoredRoot();
    var avatarContentId = rootAfterPut.getAvatarContentId();
    var coverImageContentId = rootAfterPut.getCoverImageContentId();

    var url = buildRootUrl(rootAfterPut.getId());
    requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

    verifyTwoDeletionEvents(avatarContentId, coverImageContentId);
  }

  @Test
  void doubleDeleteInlineAttachmentContentHandledCorrectly() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    var rootAfterPut = selectStoredRoot();
    var contentId = rootAfterPut.getAvatarContentId();

    var url = buildRootUrl(rootAfterPut.getId()) + "/avatar_content";
    requestHelper.executeDelete(url);
    verifySingleDeletionEvent(contentId);
    serviceHandler.clearEventContext();

    var secondDeleteResult = requestHelper.executeDelete(url);
    assertThat(secondDeleteResult.getResponse().getStatus())
        .isIn(HttpStatus.NO_CONTENT.value(), HttpStatus.OK.value());
    verifyNoAttachmentEventsCalled();
  }

  @ParameterizedTest
  @CsvSource({"avatar_status,INFECTED", "avatar_contentId,TEST"})
  void readOnlyFieldsCannotBeUpdatedViaPatchOnRoot(String field, String value) throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());
    serviceHandler.clearEventContext();
    requestHelper.resetHelper();

    var url = buildRootUrl(selectedRoot.getId());
    requestHelper.executePatchWithODataResponseAndAssertStatus(
        url, "{\"" + field + "\":\"" + value + "\"}", HttpStatus.OK);

    var rootAfterPatch = selectStoredRoot();
    assertThat(rootAfterPatch.get(field)).isNotNull().isNotEqualTo(value);
  }

  @Test
  void errorInTransactionAfterCreateRollsBackContent() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    testPersistenceHandler.setThrowExceptionOnUpdate(true);
    putInlineAttachmentContentOnRoot(
        selectedRoot.getId(), "failContent", status().is5xxServerError());

    var rootAfterError = selectStoredRoot();
    assertThat(rootAfterError.getAvatarContentId()).isNull();
    assertThat(rootAfterError.getAvatarContent()).isNull();
  }

  @Test
  void uploadWithContentDispositionHeaderExtractsFileName() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var url = buildRootUrl(selectedRoot.getId()) + "/avatar_content";
    mvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("If-Match", "*")
                .header("Content-Disposition", "attachment; filename=\"uploaded-avatar.png\"")
                .content("avatar-data".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isNoContent());

    var rootAfterPut = selectStoredRoot();
    assertThat(rootAfterPut.getAvatarFileName()).isEqualTo("uploaded-avatar.png");
    assertThat(rootAfterPut.getAvatarContentId()).isNotEmpty();
  }

  @Test
  void uploadWithSlugHeaderExtractsFileName() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var url = buildRootUrl(selectedRoot.getId()) + "/avatar_content";
    mvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("If-Match", "*")
                .header("slug", "uploaded-slug-file.txt")
                .content("slug-data".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isNoContent());

    var rootAfterPut = selectStoredRoot();
    assertThat(rootAfterPut.getAvatarFileName()).isEqualTo("uploaded-slug-file.txt");
    assertThat(rootAfterPut.getAvatarContentId()).isNotEmpty();
  }

  @Test
  void uploadWithSpecificContentTypeStoresMimeType() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var url = buildRootUrl(selectedRoot.getId()) + "/avatar_content";
    requestHelper.setContentType(MediaType.IMAGE_JPEG);
    requestHelper.executePutWithMatcher(
        url, "jpeg-data".getBytes(StandardCharsets.UTF_8), status().isNoContent());

    var rootAfterPut = selectStoredRoot();
    assertThat(rootAfterPut.getAvatarMimeType()).startsWith("image/jpeg");
    assertThat(rootAfterPut.getAvatarContentId()).isNotEmpty();
  }

  @Test
  void malwareScanStatusIsCleanAfterUpload() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    putInlineAttachmentContentOnRoot(selectedRoot.getId());

    var rootAfterPut = selectStoredRoot();
    assertThat(rootAfterPut.getAvatarStatus()).isEqualTo("Clean");
    assertThat(rootAfterPut.getAvatarScannedAt()).isNotNull();
  }

  @Test
  void putOversizedContentToCoverImageReturnsError() throws Exception {
    var root = buildRootWithoutContent();
    postServiceRoot(root);
    var selectedRoot = selectStoredRoot();

    var url = buildRootUrl(selectedRoot.getId()) + "/coverImage_content";
    byte[] oversizedContent = new byte[6 * 1024 * 1024]; // 6MB > 5MB limit
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, oversizedContent, status().is4xxClientError());
  }

  private Roots buildRootWithoutContent() {
    var root = Roots.create();
    root.setTitle("root with inline attachment");
    return root;
  }

  private Roots buildRootWithItem() {
    var root = Roots.create();
    root.setTitle("root with item");
    var items = new ArrayList<Items>();
    var item = Items.create();
    item.setTitle("item with inline attachment");
    items.add(item);
    root.setItems(items);
    return root;
  }

  private void postServiceRoot(Roots root) throws Exception {
    var url = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots";
    requestHelper.executePostWithMatcher(url, root.toJson(), status().isCreated());
  }

  private Roots selectStoredRoot() {
    var select = Select.from(Roots_.class).columns(StructuredType::_all);
    return persistenceService.run(select).single(Roots.class);
  }

  private Roots selectStoredRootWithItems() {
    var select =
        Select.from(Roots_.class)
            .columns(StructuredType::_all, root -> root.items().expand(StructuredType::_all));
    return persistenceService.run(select).single(Roots.class);
  }

  private String buildRootUrl(String rootId) {
    return MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots(" + rootId + ")";
  }

  private String buildItemUrl(String rootId, String itemId) {
    return MockHttpRequestHelper.ODATA_BASE_URL
        + "TestService/Roots("
        + rootId
        + ")/items("
        + itemId
        + ")";
  }

  private String putInlineAttachmentContentOnRoot(String rootId) throws Exception {
    return putInlineAttachmentContentOnRoot(rootId, "avatarContent");
  }

  private String putInlineAttachmentContentOnRoot(String rootId, String content) throws Exception {
    return putInlineAttachmentContentOnRoot(rootId, content, status().isNoContent());
  }

  private String putInlineAttachmentContentOnRoot(
      String rootId, String content, ResultMatcher matcher) throws Exception {
    var url = buildRootUrl(rootId) + "/avatar_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, content.getBytes(StandardCharsets.UTF_8), matcher);
    return content;
  }

  private String putCoverImageContentOnRoot(String rootId, String content) throws Exception {
    var url = buildRootUrl(rootId) + "/coverImage_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        url, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    return content;
  }

  private String putInlineAttachmentContentOnItem(String rootId, String itemId) throws Exception {
    return putInlineAttachmentContentOnItem(rootId, itemId, "iconContent");
  }

  private String putInlineAttachmentContentOnItem(String rootId, String itemId, String content)
      throws Exception {
    var url = buildItemUrl(rootId, itemId) + "/icon_content";
    requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(
        url, content.getBytes(StandardCharsets.UTF_8), status().isNoContent());
    return content;
  }

  private void verifySingleCreateEvent(String contentId, String content) {
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_READ_ATTACHMENT,
        AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    var createEvent =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvent)
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
              assertThat(((AttachmentReadEventContext) event.context()).getContentId())
                  .isEqualTo(contentId);
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

  private void verifySingleCreateAndDeleteEvent(
      String newContentId, String deletedContentId, String content) {
    waitTillExpectedHandlerMessageSize(2);
    verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
    var createEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    assertThat(createEvents)
        .first()
        .satisfies(
            event -> {
              var createContext = (AttachmentCreateEventContext) event.context();
              assertThat(createContext.getContentId()).isEqualTo(newContentId);
              assertThat(createContext.getData().getContent().readAllBytes())
                  .isEqualTo(content.getBytes(StandardCharsets.UTF_8));
            });

    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents)
        .hasSize(1)
        .first()
        .satisfies(
            event -> {
              var deleteContext = (AttachmentMarkAsDeletedEventContext) event.context();
              assertThat(deleteContext.getContentId()).isEqualTo(deletedContentId);
            });
  }

  private void verifyTwoDeletionEvents(String contentId1, String contentId2) {
    waitTillExpectedHandlerMessageSize(2);
    verifyEventContextEmptyForEvent(
        AttachmentService.EVENT_CREATE_ATTACHMENT, AttachmentService.EVENT_READ_ATTACHMENT);
    var deleteEvents =
        serviceHandler.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(2);
    assertThat(
            deleteEvents.stream()
                .anyMatch(event -> verifyDeleteEventContentIdAndUserInfo(event, contentId1)))
        .isTrue();
    assertThat(
            deleteEvents.stream()
                .anyMatch(event -> verifyDeleteEventContentIdAndUserInfo(event, contentId2)))
        .isTrue();
  }

  private boolean verifyDeleteEventContentIdAndUserInfo(
      EventContextHolder event, String contentId) {
    var ctx = (AttachmentMarkAsDeletedEventContext) event.context();
    return ctx.getContentId().equals(contentId)
        && "anonymous".equals(ctx.getDeletionUserInfo().getName())
        && Boolean.FALSE.equals(ctx.getDeletionUserInfo().getIsSystemUser());
  }

  private void verifyNoAttachmentEventsCalled() {
    assertThat(serviceHandler.getEventContext()).isEmpty();
  }

  private void verifyEventContextEmptyForEvent(String... events) {
    for (var event : events) {
      assertThat(serviceHandler.getEventContextForEvent(event)).isEmpty();
    }
  }

  private void waitTillExpectedHandlerMessageSize(int expectedSize) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> serviceHandler.getEventContext().size() >= expectedSize);
  }
}

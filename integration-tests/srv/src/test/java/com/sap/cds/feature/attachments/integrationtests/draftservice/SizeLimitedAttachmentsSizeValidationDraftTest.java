/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class SizeLimitedAttachmentsSizeValidationDraftTest extends DraftOdataRequestValidationBase {

  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
  private static final String BASE_ROOT_URL = BASE_URL + "DraftRoots";

  @Test
  void uploadContentWithin5MBLimitSucceeds() throws Exception {
    // Arrange: Create draft with sizeLimitedAttachments
    var draftRoot = createNewDraftWithSizeLimitedAttachments();
    var attachment = draftRoot.getSizeLimitedAttachments().get(0);

    // Act & Assert: Upload 3MB content (within limit) succeeds
    byte[] content = new byte[3 * 1024 * 1024]; // 3MB
    var url = buildDraftSizeLimitedAttachmentContentUrl(draftRoot.getId(), attachment.getId());
    requestHelper.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, content, status().isNoContent());
  }

  @Test
  void uploadContentExceeding5MBLimitFails() throws Exception {
    // Arrange: Create draft with sizeLimitedAttachments
    var draftRoot = createNewDraftWithSizeLimitedAttachments();
    var attachment = draftRoot.getSizeLimitedAttachments().get(0);

    // Act: Try to upload 6MB content (exceeds limit)
    byte[] content = new byte[6 * 1024 * 1024]; // 6MB
    var url = buildDraftSizeLimitedAttachmentContentUrl(draftRoot.getId(), attachment.getId());
    requestHelper.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, content, status().is(413));

    // Assert: Error response with HTTP 413 status code indicates size limit exceeded
  }

  // Helper methods
  private DraftRoots createNewDraftWithSizeLimitedAttachments() throws Exception {
    // Create new draft
    var responseRootCdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");
    var draftRoot = Struct.access(responseRootCdsData).as(DraftRoots.class);

    // Update root with title
    draftRoot.setTitle("Root with sizeLimitedAttachments");
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, draftRoot.toJson());

    // Create sizeLimitedAttachment
    var attachment = Attachments.create();
    attachment.setFileName("testFile.txt");
    attachment.setMimeType("text/plain");
    var attachmentUrl = rootUrl + "/sizeLimitedAttachments";
    var responseAttachmentCdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(
            attachmentUrl, attachment.toJson());
    var createdAttachment = Struct.access(responseAttachmentCdsData).as(Attachments.class);

    // Build result with the attachment
    draftRoot.setSizeLimitedAttachments(java.util.List.of(createdAttachment));
    return draftRoot;
  }

  private String getRootUrl(String rootId, boolean isActiveEntity) {
    return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=" + isActiveEntity + ")";
  }

  private String buildDraftSizeLimitedAttachmentContentUrl(String rootId, String attachmentId) {
    return BASE_ROOT_URL
        + "(ID="
        + rootId
        + ",IsActiveEntity=false)"
        + "/sizeLimitedAttachments(ID="
        + attachmentId
        + ",up__ID="
        + rootId
        + ",IsActiveEntity=false)"
        + "/content";
  }

  // Required abstract method implementations
  @Override
  protected void verifyContentId(String contentId, String attachmentId) {
    assertThat(contentId).isEqualTo(attachmentId);
  }

  @Override
  protected void verifyContent(InputStream attachment, String testContent) throws IOException {
    if (Objects.nonNull(testContent)) {
      assertThat(attachment.readAllBytes())
          .isEqualTo(testContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } else {
      assertThat(attachment).isNull();
    }
  }

  @Override
  protected void verifyNoAttachmentEventsCalled() {
    // no service handler - nothing to do
  }

  @Override
  protected void clearServiceHandlerContext() {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyEventContextEmptyForEvent(String... events) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyOnlyTwoCreateEvents(
      String newAttachmentContent, String newAttachmentEntityContent) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyTwoCreateAndDeleteEvents(
      String newAttachmentContent, String newAttachmentEntityContent) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyTwoReadEvents() {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyOnlyTwoDeleteEvents(
      String attachmentContentId, String attachmentEntityContentId) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyTwoUpdateEvents(
      String newAttachmentContent,
      String attachmentContentId,
      String newAttachmentEntityContent,
      String attachmentEntityContentId) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyTwoCreateAndRevertedDeleteEvents() {
    // no service handler - nothing to do
  }
}

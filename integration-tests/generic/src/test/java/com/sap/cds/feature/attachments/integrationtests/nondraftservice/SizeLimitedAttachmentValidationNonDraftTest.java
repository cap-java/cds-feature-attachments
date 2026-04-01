/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.RootEntityBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class SizeLimitedAttachmentValidationNonDraftTest extends OdataRequestValidationBase {

  @Test
  void uploadContentWithin5MBLimitSucceeds() throws Exception {
    // Arrange: Create root with sizeLimitedAttachments
    var serviceRoot = buildServiceRootWithSizeLimitedAttachments();
    postServiceRoot(serviceRoot);

    var selectedRoot = selectStoredRootWithSizeLimitedAttachments();
    var attachment = getRandomRootSizeLimitedAttachment(selectedRoot);
    attachment.setFileName("test.txt");

    // Act & Assert: Upload 3MB content (within limit) succeeds
    byte[] content = new byte[3 * 1024 * 1024]; // 3MB
    var url =
        buildNavigationSizeLimitedAttachmentUrl(selectedRoot.getId(), attachment.getId())
            + "/content";
    requestHelper.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, content, status().isNoContent());
  }

  @Test
  void uploadContentExceeding5MBLimitFails() throws Exception {
    // Arrange: Create root with sizeLimitedAttachments
    var serviceRoot = buildServiceRootWithSizeLimitedAttachments();
    postServiceRoot(serviceRoot);

    var selectedRoot = selectStoredRootWithSizeLimitedAttachments();
    var attachment = getRandomRootSizeLimitedAttachment(selectedRoot);
    attachment.setFileName("test.txt");
    // Act: Try to upload 6MB content (exceeds limit)
    byte[] content = new byte[6 * 1024 * 1024]; // 6MB
    var url =
        buildNavigationSizeLimitedAttachmentUrl(selectedRoot.getId(), attachment.getId())
            + "/content";
    requestHelper.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
    requestHelper.executePutWithMatcher(url, content, status().is(413));

    // Assert: Error response with HTTP 413 status code indicates size limit
    // exceeded
  }

  // Helper methods
  private Roots buildServiceRootWithSizeLimitedAttachments() {
    return RootEntityBuilder.create()
        .setTitle("Root with sizeLimitedAttachments")
        .addSizeLimitedAttachments(
            AttachmentsBuilder.create().setFileName("testFile.txt").setMimeType("text/plain"))
        .build();
  }

  private Roots selectStoredRootWithSizeLimitedAttachments() {
    var select =
        com.sap.cds.ql.Select.from(
                com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_
                    .class)
            .columns(r -> r._all(), r -> r.sizeLimitedAttachments().expand());

    var result = persistenceService.run(select);
    return result.single(Roots.class);
  }

  // Required abstract method implementations
  @Override
  protected void executeContentRequestAndValidateContent(String url, String content)
      throws Exception {
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .until(
            () -> {
              var response = requestHelper.executeGet(url);
              return response.getResponse().getContentAsString().equals(content);
            });

    var response = requestHelper.executeGet(url);
    assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
  }

  @Override
  protected void verifyTwoDeleteEvents(
      AttachmentEntity itemAttachmentEntityAfterChange, Attachments itemAttachmentAfterChange) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyNumberOfEvents(String event, int number) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyContentId(
      Attachments attachmentWithExpectedContent, String attachmentId, String contentId) {
    assertThat(attachmentWithExpectedContent.getContentId()).isEqualTo(attachmentId);
  }

  @Override
  protected void verifyContentAndContentId(
      Attachments attachment, String testContent, Attachments itemAttachment) throws IOException {
    assertThat(attachment.getContent().readAllBytes())
        .isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
    assertThat(attachment.getContentId()).isEqualTo(itemAttachment.getId());
  }

  @Override
  protected void verifyContentAndContentIdForAttachmentEntity(
      AttachmentEntity attachment, String testContent, AttachmentEntity itemAttachment)
      throws IOException {
    assertThat(attachment.getContent().readAllBytes())
        .isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
    assertThat(attachment.getContentId()).isEqualTo(itemAttachment.getId());
  }

  @Override
  protected void clearServiceHandlerContext() {
    // no service handler - nothing to do
  }

  @Override
  protected void clearServiceHandlerDocuments() {
    // no service handler - nothing to do
  }

  @Override
  protected void verifySingleCreateEvent(String contentId, String content) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifySingleCreateAndUpdateEvent(
      String resultContentId, String toBeDeletedContentId, String content) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifySingleDeletionEvent(String contentId) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifySingleReadEvent(String contentId) {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyNoAttachmentEventsCalled() {
    // no service handler - nothing to do
  }

  @Override
  protected void verifyEventContextEmptyForEvent(String... events) {
    // no service handler - nothing to do
  }
}

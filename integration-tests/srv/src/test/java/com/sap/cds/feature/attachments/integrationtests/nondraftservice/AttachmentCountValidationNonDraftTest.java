/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.CountValidatedRoots;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.CountValidatedRootEntityBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for @Validation.MinItems and @Validation.MaxItems annotations on non-draft
 * entities. Uses the dedicated CountValidatedRoots entity which has MinItems: 1 and MaxItems: 3.
 */
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class AttachmentCountValidationNonDraftTest extends OdataRequestValidationBase {

  private static final String COUNT_VALIDATED_ROOTS_URL =
      MockHttpRequestHelper.ODATA_BASE_URL + "TestService/CountValidatedRoots";

  // MinItems: 1, MaxItems: 3

  @Test
  void createWithZeroAttachmentsFails() throws Exception {
    // Arrange: Create root with empty countValidatedAttachments (below MinItems: 1)
    var serviceRoot =
        CountValidatedRootEntityBuilder.create()
            .setTitle("Root with zero countValidatedAttachments")
            .build();
    // Explicitly set empty list to trigger validation
    serviceRoot.setCountValidatedAttachments(new ArrayList<>());

    // Act & Assert: Should fail with HTTP 400 Bad Request
    requestHelper.executePostWithMatcher(
        COUNT_VALIDATED_ROOTS_URL, serviceRoot.toJson(), status().isBadRequest());
  }

  @Test
  void createWithOneAttachmentSucceeds() throws Exception {
    // Arrange: Create root with 1 attachment (at MinItems)
    var serviceRoot =
        CountValidatedRootEntityBuilder.create()
            .setTitle("Root with one countValidatedAttachment")
            .addCountValidatedAttachments(
                AttachmentsBuilder.create().setFileName("file1.txt").setMimeType("text/plain"))
            .build();

    // Act & Assert: Should succeed with HTTP 201 Created
    requestHelper.executePostWithMatcher(
        COUNT_VALIDATED_ROOTS_URL, serviceRoot.toJson(), status().isCreated());
  }

  @Test
  void createWithThreeAttachmentsSucceeds() throws Exception {
    // Arrange: Create root with 3 attachments (at MaxItems)
    var serviceRoot =
        CountValidatedRootEntityBuilder.create()
            .setTitle("Root with three countValidatedAttachments")
            .addCountValidatedAttachments(
                AttachmentsBuilder.create().setFileName("file1.txt").setMimeType("text/plain"),
                AttachmentsBuilder.create().setFileName("file2.txt").setMimeType("text/plain"),
                AttachmentsBuilder.create().setFileName("file3.txt").setMimeType("text/plain"))
            .build();

    // Act & Assert: Should succeed with HTTP 201 Created
    requestHelper.executePostWithMatcher(
        COUNT_VALIDATED_ROOTS_URL, serviceRoot.toJson(), status().isCreated());
  }

  @Test
  void createWithFourAttachmentsFails() throws Exception {
    // Arrange: Create root with 4 attachments (exceeds MaxItems: 3)
    var serviceRoot =
        CountValidatedRootEntityBuilder.create()
            .setTitle("Root with four countValidatedAttachments")
            .addCountValidatedAttachments(
                AttachmentsBuilder.create().setFileName("file1.txt").setMimeType("text/plain"),
                AttachmentsBuilder.create().setFileName("file2.txt").setMimeType("text/plain"),
                AttachmentsBuilder.create().setFileName("file3.txt").setMimeType("text/plain"),
                AttachmentsBuilder.create().setFileName("file4.txt").setMimeType("text/plain"))
            .build();

    // Act & Assert: Should fail with HTTP 400 Bad Request
    requestHelper.executePostWithMatcher(
        COUNT_VALIDATED_ROOTS_URL, serviceRoot.toJson(), status().isBadRequest());
  }

  @Test
  void createWithTwoAttachmentsSucceeds() throws Exception {
    // Arrange: Create root with 2 attachments (within valid range 1-3)
    var serviceRoot =
        CountValidatedRootEntityBuilder.create()
            .setTitle("Root with two countValidatedAttachments")
            .addCountValidatedAttachments(
                AttachmentsBuilder.create().setFileName("file1.txt").setMimeType("text/plain"),
                AttachmentsBuilder.create().setFileName("file2.txt").setMimeType("text/plain"))
            .build();

    // Act & Assert: Should succeed with HTTP 201 Created
    requestHelper.executePostWithMatcher(
        COUNT_VALIDATED_ROOTS_URL, serviceRoot.toJson(), status().isCreated());
  }

  @Test
  void createWithoutCountValidatedAttachmentsSucceeds() throws Exception {
    // Arrange: Create root without countValidatedAttachments composition in request
    // This should succeed because the composition is not present in request data
    var serviceRoot =
        CountValidatedRootEntityBuilder.create()
            .setTitle("Root without countValidatedAttachments")
            .build();

    // Act & Assert: Should succeed - no validation triggered when composition not in request
    requestHelper.executePostWithMatcher(
        COUNT_VALIDATED_ROOTS_URL, serviceRoot.toJson(), status().isCreated());
  }

  // Required abstract method implementations from OdataRequestValidationBase
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

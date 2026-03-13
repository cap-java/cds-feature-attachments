/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.MaxLimitedItem;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for {@code @Validation.MaxItems} on non-draft services.
 *
 * <p>These tests validate that the item count validation works correctly for deep creates on
 * non-draft (active) service entities. The annotation is defined on the Roots entity in the test
 * data model:
 *
 * <ul>
 *   <li>{@code maxLimitedAttachments}: {@code @Validation.MaxItems: 3}
 * </ul>
 *
 * <p>Note: MinItems validation is tested in unit tests only since adding
 * {@code @Validation.MinItems} to the shared Roots entity would break inherited tests from {@link
 * OdataRequestValidationBase}.
 */
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class ItemCountValidationNonDraftTest extends OdataRequestValidationBase {

  private static final String SERVICE_BASE_URL =
      MockHttpRequestHelper.ODATA_BASE_URL + "TestService/";
  private static final String ROOTS_URL = SERVICE_BASE_URL + "Roots";

  // ============================
  // MaxItems tests
  // ============================

  @Test
  void deepCreateWithTooManyAttachments_returnsError() throws Exception {
    // Arrange: Create root with 5 maxLimitedAttachments (max is 3)
    var root = Roots.create();
    root.setTitle("Root with too many maxLimited");
    List<MaxLimitedItem> items = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      var item = MaxLimitedItem.create();
      item.setName("item" + i);
      items.add(item);
    }
    root.put("maxLimitedAttachments", items);

    // Act & Assert: Should fail because 5 > 3 (max)
    requestHelper.executePostWithMatcher(ROOTS_URL, root.toJson(), status().is4xxClientError());
  }

  @Test
  void deepCreateWithinMaxItemsLimit_succeeds() throws Exception {
    // Arrange: Create root with 2 maxLimitedAttachments (max is 3)
    var root = Roots.create();
    root.setTitle("Root within maxLimit");
    List<MaxLimitedItem> items = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      var item = MaxLimitedItem.create();
      item.setName("item" + i);
      items.add(item);
    }
    root.put("maxLimitedAttachments", items);

    // Act & Assert: Should succeed because 2 <= 3
    requestHelper.executePostWithMatcher(ROOTS_URL, root.toJson(), status().isCreated());
  }

  @Test
  void deepCreateWithExactlyMaxItems_succeeds() throws Exception {
    // Arrange: Create root with exactly 3 maxLimitedAttachments (max is 3)
    var root = Roots.create();
    root.setTitle("Root at exact maxLimit");
    List<MaxLimitedItem> items = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      var item = MaxLimitedItem.create();
      item.setName("item" + i);
      items.add(item);
    }
    root.put("maxLimitedAttachments", items);

    // Act & Assert: Should succeed because 3 == 3
    requestHelper.executePostWithMatcher(ROOTS_URL, root.toJson(), status().isCreated());
  }

  @Test
  void deepCreateWithNoMaxLimitedAttachments_succeeds() throws Exception {
    // Arrange: Create root without maxLimitedAttachments (not in payload)
    var root = Roots.create();
    root.setTitle("Root without maxLimited");

    // Act & Assert: Should succeed because composition is not in payload
    requestHelper.executePostWithMatcher(ROOTS_URL, root.toJson(), status().isCreated());
  }

  // ============================
  // Required abstract method implementations
  // ============================

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

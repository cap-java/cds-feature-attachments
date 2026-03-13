/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.MaxLimitedItem;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for {@code @Validation.MaxItems} on draft services.
 *
 * <p>These tests validate that:
 *
 * <ul>
 *   <li>During draft editing (PATCH/NEW): warnings are produced (but the operation succeeds).
 *   <li>During draft activation (SAVE): errors are produced (the operation fails if violated).
 * </ul>
 *
 * <p>The annotation is defined on the Roots entity in the test data model:
 *
 * <ul>
 *   <li>{@code maxLimitedAttachments}: {@code @Validation.MaxItems: 3}
 * </ul>
 *
 * <p>Note: MinItems validation is tested in unit tests only since adding
 * {@code @Validation.MinItems} to the shared Roots entity would break inherited tests from {@link
 * DraftOdataRequestValidationBase}.
 */
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class ItemCountValidationDraftTest extends DraftOdataRequestValidationBase {

  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
  private static final String BASE_ROOT_URL = BASE_URL + "DraftRoots";

  // ============================
  // Draft editing - MaxItems (should produce warning, not reject)
  // ============================

  @Test
  void draftEditWithTooManyMaxLimitedAttachments_producesWarningNotError() throws Exception {
    // Arrange: Create new draft
    var draftRoot = createNewDraft();

    // Add 5 items to maxLimitedAttachments (max is 3)
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    for (int i = 0; i < 5; i++) {
      var item = MaxLimitedItem.create();
      item.setName("item" + i);
      var itemUrl = rootUrl + "/maxLimitedAttachments";
      // Draft operations should succeed (warnings, not errors)
      requestHelper.executePostWithODataResponseAndAssertStatusCreated(itemUrl, item.toJson());
    }

    // Assert: The draft still exists and can be read (warnings don't reject the operation)
    var response =
        requestHelper.executeGetWithSingleODataResponseAndAssertStatus(
            rootUrl, DraftRoots.class, HttpStatus.OK);
    assertThat(response).isNotNull();
  }

  // ============================
  // Draft activation - MaxItems (should produce error, reject)
  // ============================

  @Test
  void draftSaveWithTooManyMaxLimitedAttachments_returnsError() throws Exception {
    // Arrange: Create draft with 5 maxLimitedAttachments (max is 3)
    var draftRoot = createNewDraft();
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    draftRoot.setTitle("Root with too many attachments");
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, draftRoot.toJson());

    for (int i = 0; i < 5; i++) {
      var item = MaxLimitedItem.create();
      item.setName("item" + i);
      var itemUrl = rootUrl + "/maxLimitedAttachments";
      requestHelper.executePostWithODataResponseAndAssertStatusCreated(itemUrl, item.toJson());
    }

    // Act & Assert: Draft activation should fail due to MaxItems exceeded
    var draftPrepareUrl = rootUrl + "/TestDraftService.draftPrepare";
    requestHelper.executePostWithMatcher(
        draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());

    var draftActivateUrl = rootUrl + "/TestDraftService.draftActivate";
    // Draft activation should fail with a client error because max items is exceeded
    requestHelper.executePostWithMatcher(draftActivateUrl, "{}", status().is4xxClientError());
  }

  // ============================
  // Draft activation - MaxItems exactly at limit - succeeds
  // ============================

  @Test
  void draftSaveWithExactlyMaxItems_succeeds() throws Exception {
    // Arrange: Create draft with exactly 3 maxLimitedAttachments (max is 3)
    var draftRoot = createNewDraft();
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    draftRoot.setTitle("Root at exact max");
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, draftRoot.toJson());

    for (int i = 0; i < 3; i++) {
      var item = MaxLimitedItem.create();
      item.setName("item" + i);
      var itemUrl = rootUrl + "/maxLimitedAttachments";
      requestHelper.executePostWithODataResponseAndAssertStatusCreated(itemUrl, item.toJson());
    }

    // Act & Assert: Draft activation should succeed because 3 == 3
    var draftPrepareUrl = rootUrl + "/TestDraftService.draftPrepare";
    requestHelper.executePostWithMatcher(
        draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());

    var draftActivateUrl = rootUrl + "/TestDraftService.draftActivate";
    requestHelper.executePostWithMatcher(draftActivateUrl, "{}", status().isOk());
  }

  // ============================
  // Helper methods
  // ============================

  private DraftRoots createNewDraft() throws Exception {
    var responseRootCdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");
    return Struct.access(responseRootCdsData).as(DraftRoots.class);
  }

  private String getRootUrl(String rootId, boolean isActiveEntity) {
    return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=" + isActiveEntity + ")";
  }

  // ============================
  // Required abstract method implementations
  // ============================

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

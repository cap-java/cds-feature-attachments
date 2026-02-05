/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftCountValidatedRoots;
import com.sap.cds.feature.attachments.integrationtests.Application;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for @Validation.MinItems and @Validation.MaxItems annotations on draft-enabled
 * entities. Uses the dedicated DraftCountValidatedRoots entity which has MinItems: 1 and MaxItems:
 * 3.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class AttachmentCountValidationDraftTest {

  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
  private static final String BASE_ROOT_URL = BASE_URL + "DraftCountValidatedRoots";

  @Autowired private MockHttpRequestHelper requestHelper;

  // MinItems: 1, MaxItems: 3

  @Test
  void activateDraftWithZeroCountValidatedAttachmentsFails() throws Exception {
    // Arrange: Create draft with 0 countValidatedAttachments (below MinItems: 1)
    var draftRoot = createNewDraftWithCountValidatedAttachments(0);

    // Act & Assert: Activate should fail with HTTP 400 Bad Request
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    prepareAndActivateDraftExpectingFailure(rootUrl);
  }

  @Test
  void activateDraftWithOneCountValidatedAttachmentSucceeds() throws Exception {
    // Arrange: Create draft with 1 countValidatedAttachment (at MinItems)
    var draftRoot = createNewDraftWithCountValidatedAttachments(1);

    // Act & Assert: Activate should succeed
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    prepareAndActivateDraftSuccessfully(rootUrl);
  }

  @Test
  void activateDraftWithThreeCountValidatedAttachmentsSucceeds() throws Exception {
    // Arrange: Create draft with 3 countValidatedAttachments (at MaxItems)
    var draftRoot = createNewDraftWithCountValidatedAttachments(3);

    // Act & Assert: Activate should succeed
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    prepareAndActivateDraftSuccessfully(rootUrl);
  }

  @Test
  void activateDraftWithFourCountValidatedAttachmentsFails() throws Exception {
    // Arrange: Create draft with 4 countValidatedAttachments (exceeds MaxItems: 3)
    var draftRoot = createNewDraftWithCountValidatedAttachments(4);

    // Act & Assert: Activate should fail with HTTP 400 Bad Request
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    prepareAndActivateDraftExpectingFailure(rootUrl);
  }

  @Test
  void draftPatchAllowsTemporaryViolationsAboveMax() throws Exception {
    // Arrange: Create draft with valid count (2 attachments)
    var draftRoot = createNewDraftWithCountValidatedAttachments(2);
    var rootUrl = getRootUrl(draftRoot.getId(), false);

    // Act: Add more attachments to exceed MaxItems - this should succeed during draft editing
    addCountValidatedAttachment(rootUrl); // now 3
    addCountValidatedAttachment(rootUrl); // now 4 (exceeds max)

    // Assert: PATCH operations succeed (no validation during draft editing)
    // The validation only happens on activation
  }

  @Test
  void draftPatchAllowsTemporaryViolationsBelowMin() throws Exception {
    // Arrange: Create draft with valid count (2 attachments)
    var draftRoot = createNewDraftWithCountValidatedAttachments(2);
    // var rootUrl = getRootUrl(draftRoot.getId(), false);

    // Get the attachment IDs to delete them
    var attachments = draftRoot.getCountValidatedAttachments();

    // Act: Delete all attachments to go below MinItems - this should succeed during draft editing
    for (var attachment : attachments) {
      var attachmentUrl = buildCountValidatedAttachmentUrl(draftRoot.getId(), attachment.getId());
      requestHelper.executeDeleteWithMatcher(attachmentUrl, status().isNoContent());
    }

    // Assert: DELETE operations succeed (no validation during draft editing)
    // The validation only happens on activation
  }

  // Note: Tests for editing existing entities (editExistingEntityAndRemoveAllAttachmentsFails,
  // editExistingEntityAndAddTooManyAttachmentsFails) are not included because they require
  // complex draft-edit flows that have framework issues with attachment handling.
  // The validation logic is tested through create-activate scenarios above.

  @Test
  void activateDraftWithoutCountValidatedAttachmentsFails() throws Exception {
    // Arrange: Create draft without explicitly setting countValidatedAttachments
    // Since countValidatedAttachments has MinItems: 1, activation should fail
    // because the composition has 0 items (below minimum)
    var draftRoot = createNewDraftWithoutCountValidatedAttachments();

    // Act & Assert: Activate should fail because countValidatedAttachments has 0 items
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    prepareAndActivateDraftExpectingFailure(rootUrl);
  }

  // Helper methods
  private DraftCountValidatedRoots createNewDraftWithCountValidatedAttachments(int count)
      throws Exception {
    // Create new draft
    var responseRootCdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");
    var draftRoot = Struct.access(responseRootCdsData).as(DraftCountValidatedRoots.class);

    // Update root with title
    draftRoot.setTitle("Root with " + count + " countValidatedAttachments");
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, draftRoot.toJson());

    // Create countValidatedAttachments
    for (int i = 0; i < count; i++) {
      addCountValidatedAttachment(rootUrl);
    }

    // Return the draft root with attachments
    return getDraftRootWithCountValidatedAttachments(draftRoot.getId());
  }

  private DraftCountValidatedRoots createNewDraftWithoutCountValidatedAttachments()
      throws Exception {
    // Create new draft
    var responseRootCdsData =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");
    var draftRoot = Struct.access(responseRootCdsData).as(DraftCountValidatedRoots.class);

    // Update root with title only
    draftRoot.setTitle("Root without countValidatedAttachments");
    var rootUrl = getRootUrl(draftRoot.getId(), false);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, draftRoot.toJson());

    return draftRoot;
  }

  private void addCountValidatedAttachment(String rootUrl) throws Exception {
    var attachment = Attachments.create();
    attachment.setFileName("testFile.txt");
    attachment.setMimeType("text/plain");
    var attachmentUrl = rootUrl + "/countValidatedAttachments";
    requestHelper.executePostWithODataResponseAndAssertStatusCreated(
        attachmentUrl, attachment.toJson());
  }

  private DraftCountValidatedRoots getDraftRootWithCountValidatedAttachments(String rootId)
      throws Exception {
    var url =
        BASE_ROOT_URL
            + "(ID="
            + rootId
            + ",IsActiveEntity=false)?$expand=countValidatedAttachments";
    var response =
        requestHelper.executeGetWithSingleODataResponseAndAssertStatus(
            url, DraftCountValidatedRoots.class, HttpStatus.OK);
    return response;
  }

  private String getRootUrl(String rootId, boolean isActiveEntity) {
    return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=" + isActiveEntity + ")";
  }

  private String buildCountValidatedAttachmentUrl(String rootId, String attachmentId) {
    return BASE_ROOT_URL
        + "(ID="
        + rootId
        + ",IsActiveEntity=false)"
        + "/countValidatedAttachments(ID="
        + attachmentId
        + ",up__ID="
        + rootId
        + ",IsActiveEntity=false)";
  }

  private void prepareAndActivateDraftSuccessfully(String rootUrl) throws Exception {
    var draftPrepareUrl = rootUrl + "/TestDraftService.draftPrepare";
    var draftActivateUrl = rootUrl + "/TestDraftService.draftActivate";
    requestHelper.executePostWithMatcher(
        draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());
    requestHelper.executePostWithMatcher(draftActivateUrl, "{}", status().isOk());
  }

  private void prepareAndActivateDraftExpectingFailure(String rootUrl) throws Exception {
    var draftPrepareUrl = rootUrl + "/TestDraftService.draftPrepare";
    var draftActivateUrl = rootUrl + "/TestDraftService.draftActivate";
    requestHelper.executePostWithMatcher(
        draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());
    requestHelper.executePostWithMatcher(draftActivateUrl, "{}", status().isBadRequest());
  }
}

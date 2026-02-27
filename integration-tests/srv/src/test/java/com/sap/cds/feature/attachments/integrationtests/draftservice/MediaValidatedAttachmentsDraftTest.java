/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
public class MediaValidatedAttachmentsDraftTest extends DraftOdataRequestValidationBase {

  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
  private static final String BASE_ROOT_URL = BASE_URL + "DraftRoots";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    requestHelper.setContentType(MediaType.APPLICATION_JSON);
  }

  @ParameterizedTest
  @CsvSource({
    "test.png,201",
    "test.jpeg,201",
    "test.pdf,415",
    "test.txt,415",
    "'',400",
    "'   ',400",
    ".gitignore,415",
    ".env,415",
    ".hiddenfile,415"
  })
  void shouldValidateMediaType_whenCreatingAttachmentInDraft(String fileName, int expectedStatus)
      throws Exception {
    String rootId = createDraftRootAndReturnId();
    String metadata = objectMapper.writeValueAsString(Map.of("fileName", fileName));

    requestHelper.executePostWithMatcher(
        buildDraftAttachmentCreationUrl(rootId), metadata, status().is(expectedStatus));
  }

  private String buildDraftAttachmentCreationUrl(String rootId) {
    return BASE_ROOT_URL
        + "(ID="
        + rootId
        + ",IsActiveEntity=false)"
        + "/mediaValidatedAttachments";
  }

  @Test
  void shouldPass_whenFileNameMissing_inDraft() throws Exception {
    String rootId = createDraftRootAndReturnId();
    String metadata = "{}";
    requestHelper.executePostWithMatcher(
        buildDraftAttachmentCreationUrl(rootId), metadata, status().isCreated());
  }

  // Helper methods
  private String createDraftRootAndReturnId() throws Exception {
    CdsData response =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");

    DraftRoots draftRoot = Struct.access(response).as(DraftRoots.class);
    String payload = objectMapper.writeValueAsString(Map.of("title", "Draft"));
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        getRootUrl(draftRoot.getId(), false), payload);

    return draftRoot.getId();
  }

  private String getRootUrl(String rootId, boolean isActiveEntity) {
    return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=" + isActiveEntity + ")";
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
    // Implementation not required for this test
  }

  @Override
  protected void clearServiceHandlerContext() {
    // Implementation not required for this test
  }

  @Override
  protected void verifyEventContextEmptyForEvent(String... events) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyOnlyTwoCreateEvents(
      String newAttachmentContent, String newAttachmentEntityContent) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyTwoCreateAndDeleteEvents(
      String newAttachmentContent, String newAttachmentEntityContent) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyTwoReadEvents() {
    // Implementation not required for this test
  }

  @Override
  protected void verifyOnlyTwoDeleteEvents(
      String attachmentContentId, String attachmentEntityContentId) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyTwoUpdateEvents(
      String newAttachmentContent,
      String attachmentContentId,
      String newAttachmentEntityContent,
      String attachmentEntityContentId) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyTwoCreateAndRevertedDeleteEvents() {
    // Implementation not required for this test
  }
}

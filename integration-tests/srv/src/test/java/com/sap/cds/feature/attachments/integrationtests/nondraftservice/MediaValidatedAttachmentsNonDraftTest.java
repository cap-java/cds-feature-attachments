/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.RootEntityBuilder;
import com.sap.cds.ql.Select;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class MediaValidatedAttachmentsNonDraftTest extends OdataRequestValidationBase {
  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots";
  private static final String MEDIA_VALIDATED_ATTACHMENTS = "mediaValidatedAttachments";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  protected void postServiceRoot(Roots serviceRoot) throws Exception {
    String url = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots";
    requestHelper.executePostWithMatcher(url, serviceRoot.toJson(), status().isCreated());
  }

  private Roots selectStoredRootWithMediaValidatedAttachments() {
    Select<Roots_> select =
        Select.from(Roots_.class)
            .columns(r -> r._all(), r -> r.mediaValidatedAttachments().expand());

    Result result = persistenceService.run(select);
    return result.single(Roots.class);
  }

  @BeforeEach
  void setup() {
    requestHelper.setContentType(MediaType.APPLICATION_JSON);
  }

  @ParameterizedTest
  @CsvSource({
    "image.jpg,image/jpeg,201",
    "image.png,image/png,201",
    "document.pdf,application/pdf,415",
    "notes.txt,text/plain,415"
  })
  void shouldValidateMediaTypes(String fileName, String mediaType, int expectedStatus)
      throws Exception {
    String rootId = createRootAndReturnId();
    String attachmentMetadata = createAttachmentMetadata(fileName);

    requestHelper.executePostWithMatcher(
        createUrl(rootId, MEDIA_VALIDATED_ATTACHMENTS),
        attachmentMetadata,
        status().is(expectedStatus));
  }

  @Test
  void shouldRejectAttachment_whenFileNameIsEmpty() throws Exception {
    String rootId = createRootAndReturnId();
    String fileName = "";
    String attachmentMetadata = createAttachmentMetadata(fileName);

    requestHelper.executePostWithMatcher(
        createUrl(rootId, MEDIA_VALIDATED_ATTACHMENTS),
        attachmentMetadata,
        status().isBadRequest());
  }

  @Test
  void shouldAcceptUppercaseExtension_whenMimeTypeIsAllowed() throws Exception {
    String rootId = createRootAndReturnId();
    String attachmentMetadata = createAttachmentMetadata("IMAGE.JPG");

    requestHelper.executePostWithMatcher(
        createUrl(rootId, MEDIA_VALIDATED_ATTACHMENTS), attachmentMetadata, status().isCreated());
  }

  @Test
  void shouldAcceptMixedCaseExtension() throws Exception {
    String rootId = createRootAndReturnId();
    String attachmentMetadata = createAttachmentMetadata("image.JpEg");

    requestHelper.executePostWithMatcher(
        createUrl(rootId, MEDIA_VALIDATED_ATTACHMENTS), attachmentMetadata, status().isCreated());
  }

  @Test
  void shouldRejectAttachment_whenFileHasNoExtension() throws Exception {
    String rootId = createRootAndReturnId();
    String attachmentMetadata = createAttachmentMetadata("filename");

    requestHelper.executePostWithMatcher(
        createUrl(rootId, MEDIA_VALIDATED_ATTACHMENTS),
        attachmentMetadata,
        status().isBadRequest());
  }

  @Test
  void shouldRejectHiddenFile_whenFileStartsWithDot() throws Exception {
    String rootId = createRootAndReturnId();
    String attachmentMetadata = createAttachmentMetadata(".gitignore");

    requestHelper.executePostWithMatcher(
        createUrl(rootId, MEDIA_VALIDATED_ATTACHMENTS),
        attachmentMetadata,
        status().isUnsupportedMediaType());
  }

  @ParameterizedTest
  @CsvSource({
    // valid cases
    "'test1.jpeg|test2.jpeg',201",
    // invalid media types
    "'test.pdf',415",
    "'test1.jpeg|test2.pdf',415",
    // invalid filenames
    "'',400",
    "'   ',400",
    // edge cases
    "'.gitignore',415"
  })
  void shouldValidateMediaTypes_forMultipleAttachments(String fileNames, int expectedStatus)
      throws Exception {
    String payload = buildPayload(fileNames);
    requestHelper.executePostWithMatcher(BASE_URL, payload, status().is(expectedStatus));
  }

  @Test
  void shouldAcceptWhenMediaValidatedAttachments_hasNoAttachments() throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", "Hello World!");
    payload.put("mediaValidatedAttachments", List.of());

    String payloadStr = objectMapper.writeValueAsString(payload);
    requestHelper.executePostWithMatcher(BASE_URL, payloadStr, status().is(201));
  }

  @Test
  void shouldAcceptDeepCreate_whenMixedValidAndAllValidAttachments() throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", "Hello World!");
    payload.put(
        "mediaValidatedAttachments",
        List.of(Map.of("fileName", "test1.jpeg"), Map.of("fileName", "test2.jpeg")));

    payload.put("mimeValidatedAttachments", List.of(Map.of("fileName", "test3.pdf")));

    requestHelper.executePostWithMatcher(
        BASE_URL, objectMapper.writeValueAsString(payload), status().isCreated());
  }

  @Test
  void shouldRejectDeepCreate_whenMixedValidAndInvalidAttachments() throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", "Hello World!");
    payload.put(
        "mediaValidatedAttachments",
        List.of(Map.of("fileName", "test1.pdf"), Map.of("fileName", "test2.jpeg")));

    payload.put("mimeValidatedAttachments", List.of(Map.of("fileName", "test3.pdf")));

    requestHelper.executePostWithMatcher(
        BASE_URL, objectMapper.writeValueAsString(payload), status().isUnsupportedMediaType());
  }

  private String createRootAndReturnId() throws Exception {
    // Build the initial Java object.. Root
    Roots serviceRoot = buildServiceRoot();

    // POST the root object to the server to create it in the database
    postServiceRoot(serviceRoot);

    // Read the newly created entity back from the database
    Roots selectedRoot = selectStoredRootWithMediaValidatedAttachments();

    return selectedRoot.getId();
  }

  private String buildPayload(String fileNames) throws JsonProcessingException {
    List<Map<String, String>> attachments = new ArrayList<>();
    fileNames = fileNames.replaceAll("^'+|'+$", "");
    for (String name : fileNames.split("\\|")) {
      attachments.add(Map.of("fileName", name));
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", "Hello World!");
    payload.put("mediaValidatedAttachments", attachments);

    return objectMapper.writeValueAsString(payload);
  }

  private String createUrl(String rootId, String path) {
    return BASE_URL + "(" + rootId + ")" + (path == null || path.isBlank() ? "" : "/" + path);
  }

  private String createAttachmentMetadata(String fileName) throws JsonProcessingException {
    return objectMapper.writeValueAsString(Map.of("fileName", fileName));
  }

  // helper method
  private Roots buildServiceRoot() {
    return RootEntityBuilder.create().setTitle("Root").build();
  }

  // Override abstract methods from OdataRequestValidationBase

  @Override
  protected void executeContentRequestAndValidateContent(String url, String content)
      throws Exception {
    // Implementation not required for this test
  }

  @Override
  protected void verifyContentId(
      Attachments attachmentWithExpectedContent, String attachmentId, String contentId) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyContentAndContentId(
      Attachments attachment, String testContent, Attachments itemAttachment) {
    // Implementation not required for this test
  }

  @Override
  protected void verifyContentAndContentIdForAttachmentEntity(
      AttachmentEntity attachment, String testContent, AttachmentEntity itemAttachment) {
    // Implementation not required for this test
  }

  @Override
  public void verifySingleCreateAndUpdateEvent(String arg1, String arg2, String arg3) {
    // Implementation not required for this test
  }

  @Override
  public void clearServiceHandlerContext() {
    // Implementation not required for this test
  }

  @Override
  public void verifySingleReadEvent(String arg) {
    // Implementation not required for this test
  }

  @Override
  public void verifyTwoDeleteEvents(AttachmentEntity entity, Attachments attachments) {
    // Implementation not required for this test
  }

  @Override
  public void clearServiceHandlerDocuments() {
    // Implementation not required for this test
  }

  @Override
  public void verifyEventContextEmptyForEvent(String... args) {
    // Implementation not required for this test
  }

  @Override
  public void verifyNoAttachmentEventsCalled() {
    // Implementation not required for this test
  }

  @Override
  public void verifyNumberOfEvents(String arg, int count) {
    // Implementation not required for this test
  }

  @Override
  public void verifySingleCreateEvent(String arg1, String arg2) {
    // Implementation not required for this test
  }

  @Override
  public void verifySingleDeletionEvent(String arg) {
    // Implementation not required for this test
  }
}

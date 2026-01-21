/*
* © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
*/
package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.RootEntityBuilder;
import com.sap.cds.ql.Select;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class AcceptedMediaTypesAttachmentsValidationNonDraftTest extends OdataRequestValidationBase {

    @Test
    void uploadingAllowedMediaTypeShouldSucceed() throws Exception {
        // Arrange: Create root with mediaValidatedAttachments
        Roots serviceRoot = buildServiceRootWithMediaValidatedAttachments("test.jpeg", MediaType.IMAGE_JPEG.toString());
        postServiceRoot(serviceRoot);

        Roots selectedRoot = selectStoredRootWithMediaValidatedAttachments();
        Attachments attachment = selectedRoot.getMediaValidatedAttachments().get(0);

        // Act & Assert: Upload content with allowed media type
        String url = buildNavigationMediaValidationAttachmentUrl(selectedRoot.getId(), attachment.getId()) + "/content";
        requestHelper.setContentType(MediaType.IMAGE_JPEG);

        requestHelper.executePutWithMatcher(
                url,
                "fake-jpeg-content".getBytes(StandardCharsets.UTF_8),
                status().isNoContent());
    }

    @Test
    void uploadingNotAllowedMediaTypeShouldFail() throws Exception {
        Roots serviceRoot = buildServiceRootWithMediaValidatedAttachments("test.pdf",
                MediaType.APPLICATION_PDF.toString());
        postServiceRoot(serviceRoot);

        Roots selectedRoot = selectStoredRootWithMediaValidatedAttachments();
        Attachments attachment = selectedRoot.getMediaValidatedAttachments().get(0);

        // Act & Assert: Upload with disallowed media type fails
        String url = buildNavigationMediaValidationAttachmentUrl(selectedRoot.getId(), attachment.getId()) + "/content";
        requestHelper.setContentType(MediaType.APPLICATION_PDF);

        requestHelper.executePutWithMatcher(
                url,
                "fake-jpeg-content".getBytes(StandardCharsets.UTF_8),
                status().isUnsupportedMediaType());
    }

    // Helper methods
    private String buildNavigationMediaValidationAttachmentUrl(String rootId, String attachmentId) {
        return "/odata/v4/TestService/Roots("
                + rootId
                + ")/mediaValidatedAttachments(ID="
                + attachmentId
                + ",up__ID="
                + rootId
                + ")";
    }

    private Roots buildServiceRootWithMediaValidatedAttachments(String fileName, String mimeType) {
        return RootEntityBuilder.create()
                .setTitle("Root with mediaValidatedAttachments")
                .addMediaValidatedAttachments(
                        AttachmentsBuilder.create().setFileName(fileName).setMimeType(mimeType))
                .build();
    }

    private Roots selectStoredRootWithMediaValidatedAttachments() {
        Select<Roots_> select = Select.from(
                Roots_.class)
                .columns(r -> r._all(), r -> r.mediaValidatedAttachments().expand());

        Result result = persistenceService.run(select);
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
                            MvcResult response = requestHelper.executeGet(url);
                            return response.getResponse().getContentAsString().equals(content);
                        });

        MvcResult response = requestHelper.executeGet(url);
        assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
    }

    // Required abstract method implementations

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
    public void verifySingleCreateAndUpdateEvent(String arg1, String arg2, String arg3) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void clearServiceHandlerContext() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifySingleReadEvent(String arg) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyTwoDeleteEvents(AttachmentEntity entity, Attachments attachments) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void clearServiceHandlerDocuments() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyEventContextEmptyForEvent(String... args) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyNoAttachmentEventsCalled() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyNumberOfEvents(String arg, int count) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifySingleCreateEvent(String arg1, String arg2) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifySingleDeletionEvent(String arg) {
        // No service handler is present, so no actions are required.
    }

}

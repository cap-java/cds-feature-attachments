/*
* © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
*/
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class AcceptedMediaTypesAttachmentsValidationDraftTest extends DraftOdataRequestValidationBase {

    private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
    private static final String BASE_ROOT_URL = BASE_URL + "DraftRoots";

    @Test
    void uploadingAllowedMediaTypeShouldSucceed() throws Exception {
        DraftRoots draftRoot = createNewDraftWithMediaValidatedAttachment(
                "Root with mediaValidatedAttachments",
                "test.jpeg",
                MediaType.IMAGE_JPEG.toString());
        Attachments attachment = draftRoot.getMediaValidatedAttachments().get(0);

        String url = buildDraftMediaValidatedAttachmentContentUrl(
                draftRoot.getId(),
                attachment.getId());

        requestHelper.setContentType(MediaType.IMAGE_JPEG);

        requestHelper.executePutWithMatcher(
                url,
                "fake-jpeg-content".getBytes(StandardCharsets.UTF_8),
                status().isNoContent());
    }

    @Test
    void uploadingNotAllowedMediaTypeShouldFail() throws Exception {
        DraftRoots draftRoot = createNewDraftWithMediaValidatedAttachment(
                "Root with pdf attachment",
                "test.pdf",
                MediaType.APPLICATION_PDF.toString());
        Attachments attachment = draftRoot.getMediaValidatedAttachments().get(0);

        String url = buildDraftMediaValidatedAttachmentContentUrl(
                draftRoot.getId(),
                attachment.getId());

        requestHelper.setContentType(MediaType.APPLICATION_PDF);

        requestHelper.executePutWithMatcher(
                url,
                "fake-pdf-content".getBytes(StandardCharsets.UTF_8),
                status().isUnsupportedMediaType());
    }

    // helper methods
    private DraftRoots createNewDraftWithMediaValidatedAttachment(String title, String fileName, String mimeType)
            throws Exception {

        // Create new draft root
        CdsData responseRootCdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL,
                "{}");
        DraftRoots draftRoot = Struct.access(responseRootCdsData).as(DraftRoots.class);
        draftRoot.setTitle(title);
        String rootUrl = getRootUrl(draftRoot.getId(), false);
        requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, draftRoot.toJson());

        // Create attachment
        Attachments attachment = Attachments.create();
        attachment.setFileName(fileName);
        attachment.setMimeType(mimeType);

        String attachmentUrl = rootUrl + "/mediaValidatedAttachments";
        CdsData responseAttachmentCdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(
                attachmentUrl, attachment.toJson());

        Attachments createdAttachment = Struct.access(responseAttachmentCdsData).as(Attachments.class);

        // Attach to draft root
        draftRoot.setMediaValidatedAttachments(java.util.List.of(createdAttachment));
        return draftRoot;
    }

    private String getRootUrl(String rootId, boolean isActiveEntity) {
        return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=" + isActiveEntity + ")";
    }

    private String buildDraftMediaValidatedAttachmentContentUrl(String rootId, String attachmentId) {
        return BASE_ROOT_URL
                + "(ID="
                + rootId
                + ",IsActiveEntity=false)"
                + "/mediaValidatedAttachments(ID="
                + attachmentId
                + ",up__ID="
                + rootId
                + ",IsActiveEntity=false)"
                + "/content";
    }

    // methods we do not need, but should override

    @Override
    public void verifyTwoCreateAndRevertedDeleteEvents() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyTwoCreateAndDeleteEvents(String param1, String param2) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyEventContextEmptyForEvent(String... params) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void clearServiceHandlerContext() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyOnlyTwoCreateEvents(String param1, String param2) {
        // No service handler is present, so no actions are required.
    }

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
    public void verifyTwoReadEvents() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyOnlyTwoDeleteEvents(String param1, String param2) {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyNoAttachmentEventsCalled() {
        // No service handler is present, so no actions are required.
    }

    @Override
    public void verifyTwoUpdateEvents(String param1, String param2, String param3, String param4) {
        // No service handler is present, so no actions are required.
    }
}

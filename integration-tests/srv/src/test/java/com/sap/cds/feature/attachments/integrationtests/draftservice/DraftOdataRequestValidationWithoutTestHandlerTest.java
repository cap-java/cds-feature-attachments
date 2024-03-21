package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;

@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class DraftOdataRequestValidationWithoutTestHandlerTest extends DraftOdataRequestValidationBase {

	@Test
	void serviceHandlerIsNull() {
		assertThat(serviceHandler).isNull();
	}

	@Override
	protected void verifyDocumentId(String documentId, String attachmentId) {
		assertThat(documentId).isEqualTo(attachmentId);
	}

	@Override
	protected void verifyContent(InputStream attachment, String testContent) throws IOException {
		if (Objects.nonNull(testContent)) {
			assertThat(attachment.readAllBytes()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
		} else {
			assertThat(attachment).isNull();
		}
	}

	@Override
	protected void verifyNoAttachmentEventsCalled() {
		//	no service handler - nothing to do
	}

	@Override
	protected void clearServiceHandlerContext() {
		//	no service handler - nothing to do
	}

	@Override
	protected void verifyEventContextEmptyForEvent(String... events) {
		//	no service handler - nothing to do
	}

	@Override
	protected void verifyTwoCreateEvents(String newAttachmentContent, String newAttachmentEntityContent) {
		//	no service handler - nothing to do
	}

	@Override
	protected void verifyTwoReadEvents() {
		//	no service handler - nothing to do
	}

	@Override
	protected void verifyTwoDeleteEvents(String attachmentDocumentId, String attachmentEntityDocumentId) {
		//	no service handler - nothing to do
	}

	@Override
	protected void verifyTwoUpdateEvents(String newAttachmentContent, String attachmentDocumentId, String newAttachmentEntityContent, String attachmentEntityDocumentId) {
		//	no service handler - nothing to do
	}

}

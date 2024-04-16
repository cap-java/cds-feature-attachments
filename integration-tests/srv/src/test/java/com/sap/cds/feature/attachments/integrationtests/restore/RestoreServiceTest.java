package com.sap.cds.feature.attachments.integrationtests.restore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.com.sap.attachments.restoreattachments.RestoreAttachmentsContext;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.com.sap.attachments.restoreattachments.RestoreAttachments_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_ENABLED)
class RestoreServiceTest {

	@Autowired
	protected MockHttpRequestHelper requestHelper;

	@Autowired
	protected TestPluginAttachmentsServiceHandler serviceHandler;

	@BeforeEach
	void setup() {
		serviceHandler.clearEventContext();
	}

	@AfterEach
	void teardown() {
		serviceHandler.clearEventContext();
	}

	@Test
	void restoreAttachmentsCalled() throws Exception {
		var timestamp = Instant.now();

		var url = MockHttpRequestHelper.ODATA_BASE_URL + RestoreAttachments_.CDS_NAME + "/" + RestoreAttachmentsContext.CDS_NAME;
		requestHelper.executePostWithMatcher(url, "{\"restoreTimestamp\":\"" + timestamp + "\"}", status().isNoContent());

		var contexts = serviceHandler.getEventContext();
		assertThat(contexts).hasSize(1);
		var event = contexts.get(0).event();
		var context = (AttachmentRestoreEventContext) contexts.get(0).context();
		assertThat(event).isEqualTo(AttachmentService.EVENT_RESTORE);
		assertThat(context.getRestoreTimestamp()).isEqualTo(timestamp);
	}

	@Test
	void restoreAttachmentsCalledWithNotAuthorizedUser() throws Exception {
		var timestamp = Instant.now();

		var url = MockHttpRequestHelper.ODATA_BASE_URL + RestoreAttachments_.CDS_NAME + "/" + RestoreAttachmentsContext.CDS_NAME;
		var response = requestHelper.executePost(url, "{\"restoreTimestamp\":\"" + timestamp + "\"}");

		assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
	}

}

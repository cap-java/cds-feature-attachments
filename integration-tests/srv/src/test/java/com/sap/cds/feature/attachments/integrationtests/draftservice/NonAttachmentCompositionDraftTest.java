/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test for GitHub issue cap/issues#20410.
 *
 * <p>Creating a non-attachment composition child (Contributors) via DRAFT_NEW must not crash with
 * "No element with name 'content'" even though the parent entity (Roots) also has attachment
 * compositions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class NonAttachmentCompositionDraftTest {

  private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
  private static final String BASE_ROOT_URL = BASE_URL + "DraftRoots";

  @Autowired private MockHttpRequestHelper requestHelper;
  @Autowired private TableDataDeleter dataDeleter;

  @AfterEach
  void teardown() {
    dataDeleter.deleteData(DraftRoots_.CDS_NAME, DraftRoots_.CDS_NAME + "_drafts");
    requestHelper.resetHelper();
  }

  @Test
  void createNonAttachmentCompositionInDraft_shouldSucceed() throws Exception {
    String rootId = createDraftRootAndReturnId();

    String contributorsUrl =
        BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=false)/contributors";

    requestHelper.executePostWithMatcher(
        contributorsUrl, "{\"name\":\"Alice\"}", status().isCreated());
  }

  private String createDraftRootAndReturnId() throws Exception {
    CdsData response =
        requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");
    DraftRoots draftRoot = Struct.access(response).as(DraftRoots.class);
    requestHelper.executePatchWithODataResponseAndAssertStatusOk(
        getRootUrl(draftRoot.getId()), "{\"title\":\"test\"}");
    return draftRoot.getId();
  }

  private String getRootUrl(String rootId) {
    return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=false)";
  }
}

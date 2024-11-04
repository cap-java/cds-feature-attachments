package com.sap.cds.feature.attachments.integrationtests.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.sap.cds.Struct;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.DraftRoots_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.Items;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testdraftservice.TestDraftService_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPersistenceHandler;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.services.persistence.PersistenceService;

@SpringBootTest
@AutoConfigureMockMvc
abstract class DraftOdataRequestValidationBase {

	protected static final Logger logger = LoggerFactory.getLogger(DraftOdataRequestValidationBase.class);
	private static final String BASE_URL = MockHttpRequestHelper.ODATA_BASE_URL + "TestDraftService/";
	private static final String BASE_ROOT_URL = BASE_URL + "DraftRoots";
	@Autowired(required = false)
	protected TestPluginAttachmentsServiceHandler serviceHandler;
	@Autowired
	protected MockHttpRequestHelper requestHelper;
	@Autowired
	private PersistenceService persistenceService;
	@Autowired
	private TableDataDeleter dataDeleter;
	@Autowired
	private TestPersistenceHandler testPersistenceHandler;

	@AfterEach
	void teardown() {
		dataDeleter.deleteData(DraftRoots_.CDS_NAME, DraftRoots_.CDS_NAME + "_drafts", "cds.outbox.Messages");
		requestHelper.resetHelper();
		clearServiceHandlerContext();
		testPersistenceHandler.reset();
	}

	@Test
	void deepCreateWorks() throws Exception {
		var testContentAttachment = "testContent attachment";
		var testContentAttachmentEntity = "testContent attachmentEntity";

		var selectedRoot = deepCreateAndActivate(testContentAttachment, testContentAttachmentEntity);

		assertThat(selectedRoot.getIsActiveEntity()).isTrue();

		var selectedAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var selectedAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		verifyContentId(selectedAttachment.getContentId(), selectedAttachment.getId());
		assertThat(selectedAttachment.getFileName()).isEqualTo("itemAttachment.txt");
		assertThat(selectedAttachment.getMimeType()).contains("text/plain");
		verifyContent(selectedAttachment.getContent(), testContentAttachment);
		verifyContentId(selectedAttachmentEntity.getContentId(), selectedAttachmentEntity.getId());
		assertThat(selectedAttachmentEntity.getFileName()).isEqualTo("itemAttachmentEntity.txt");
		assertThat(selectedAttachmentEntity.getMimeType()).contains("image/jpeg");
		verifyContent(selectedAttachmentEntity.getContent(), testContentAttachmentEntity);
		verifyOnlyTwoCreateEvents(testContentAttachment, testContentAttachmentEntity);
	}

	@Test
	void contentCanBeReadFromDraft() throws Exception {
		var testContentAttachment = "testContent attachment";
		var testContentAttachmentEntity = "testContent attachmentEntity";

		var root = deepCreateAndActivate(testContentAttachment, testContentAttachmentEntity);

		var selectedRoot = selectStoredRootData(root);
		assertThat(selectedRoot.getItems().get(0).getAttachments()).hasSize(1).first().satisfies(
				attachment -> verifyContent(attachment.getContent(), testContentAttachment));
		assertThat(selectedRoot.getItems().get(0).getAttachmentEntities()).hasSize(1).first().satisfies(
				attachment -> verifyContent(attachment.getContent(), testContentAttachmentEntity));
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var attachmentUrl = getAttachmentBaseUrl(selectedRoot.getItems().get(0).getId(), selectedRoot.getItems().get(0)
				.getAttachments().get(0).getId(),	false) + "/content";
		var attachmentEntityUrl = getAttachmentEntityBaseUrl(selectedRoot.getItems().get(0).getAttachmentEntities().get(0)
				.getId(), false) + "/content";

		Awaitility.await().atMost(30, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
			var attachmentResponse = requestHelper.executeGet(attachmentUrl);
			var attachmentEntityResponse = requestHelper.executeGet(attachmentEntityUrl);
			var attachmentResponseContent = getResponseContent(attachmentResponse);
			var attachmentEntityResponseContent = getResponseContent(attachmentEntityResponse);
			var result = attachmentResponseContent.equals(testContentAttachment) && attachmentEntityResponseContent.equals(
					testContentAttachmentEntity);
			if (!result) {
				logger.info(
						"Attachment response content: {}, Attachment Test Content: {}, Attachment Entity response content: {}, Attachment Entity Test Content: {}",
						attachmentResponseContent, testContentAttachment, attachmentEntityResponseContent, testContentAttachmentEntity);
			}
			return result;
		});
		clearServiceHandlerContext();

		var attachmentResponse = requestHelper.executeGet(attachmentUrl);
		assertThat(attachmentResponse.getResponse().getContentAsString()).isEqualTo(testContentAttachment);
		var attachmentEntityResponse = requestHelper.executeGet(attachmentEntityUrl);
		assertThat(attachmentEntityResponse.getResponse().getContentAsString()).isEqualTo(testContentAttachmentEntity);
		verifyTwoReadEvents();
	}

	@Test
	void deleteAttachmentAndActivateDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		var attachmentDeleteUrl = getAttachmentBaseUrl(selectedRoot.getItems().get(0).getId(), itemAttachment.getId(), false);
		var attachmentEntityDeleteUrl = getAttachmentEntityBaseUrl(itemAttachmentEntity.getId(), false);

		requestHelper.executeDeleteWithMatcher(attachmentDeleteUrl, status().isNoContent());
		requestHelper.executeDeleteWithMatcher(attachmentEntityDeleteUrl, status().isNoContent());
		verifyNoAttachmentEventsCalled();

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));

		var selectedRootAfterDelete = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterDelete.getItems().get(0).getAttachments()).isEmpty();
		assertThat(selectedRootAfterDelete.getItems().get(0).getAttachmentEntities()).isEmpty();
		verifyOnlyTwoDeleteEvents(itemAttachment.getContentId(), itemAttachmentEntity.getContentId());
	}

	@Test
	void updateAttachmentAndActivateDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		var changedAttachmentFileName = "changedAttachmentFileName.txt";
		var changedAttachmentEntityFileName = "changedAttachmentEntityFileName.txt";

		updateFileName(selectedRoot, itemAttachment, itemAttachmentEntity, changedAttachmentFileName,
				changedAttachmentEntityFileName);

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterUpdate = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterUpdate.getItems().get(0).getAttachments().get(0).getFileName()).isEqualTo(
				changedAttachmentFileName);
		assertThat(selectedRootAfterUpdate.getItems().get(0).getAttachmentEntities().get(0).getFileName()).isEqualTo(
				changedAttachmentEntityFileName);
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void updateAttachmentAndCancelDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		var originAttachmentFileName = itemAttachment.getFileName();
		var originAttachmentEntityFileName = itemAttachmentEntity.getFileName();

		updateFileName(selectedRoot, itemAttachment, itemAttachmentEntity, "changedAttachmentFileName.txt",
				"changedAttachmentEntityFileName.txt");

		cancelDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterUpdate = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterUpdate.getItems().get(0).getAttachments().get(0).getFileName()).isEqualTo(
				originAttachmentFileName);
		assertThat(selectedRootAfterUpdate.getItems().get(0).getAttachmentEntities().get(0).getFileName()).isEqualTo(
				originAttachmentEntityFileName);
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void createAttachmentAndActivateDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0);

		var newAttachmentContent = "new attachment content";
		createAttachmentWithContent(newAttachmentContent, itemAttachment.getId());
		var newAttachmentEntityContent = "new attachmentEntity content";
		createAttachmentEntityWithContent(newAttachmentEntityContent, itemAttachment);

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterCreate = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachments()).hasSize(2);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachmentEntities()).hasSize(2);
		verifyOnlyTwoCreateEvents(newAttachmentContent, newAttachmentEntityContent);
	}

	@Test
	void createAttachmentAndCancelDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0);

		var newAttachmentContent = "new attachment content";
		createAttachmentWithContent(newAttachmentContent, itemAttachment.getId());
		var newAttachmentEntityContent = "new attachmentEntity content";
		createAttachmentEntityWithContent(newAttachmentEntityContent, itemAttachment);

		cancelDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterCreate = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachments()).hasSize(1);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachmentEntities()).hasSize(1);
		verifyTwoCreateAndDeleteEvents(newAttachmentContent, newAttachmentEntityContent);
	}

	@Test
	void deleteContentInDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment for delete",
				"testContent attachmentEntity for delete");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());
		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		deleteContent(selectedRoot, itemAttachment, itemAttachmentEntity);
		verifyNoAttachmentEventsCalled();

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterDelete = selectStoredRootData(selectedRoot);
		verifyContent(selectedRootAfterDelete.getItems().get(0).getAttachments().get(0).getContent(), null);
		verifyContent(selectedRootAfterDelete.getItems().get(0).getAttachmentEntities().get(0).getContent(), null);
		verifyOnlyTwoDeleteEvents(itemAttachment.getContentId(), itemAttachmentEntity.getContentId());
	}

	@Test
	void doNotDeleteContentInCancelledDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		deleteContent(selectedRoot, itemAttachment, itemAttachmentEntity);

		cancelDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterDelete = selectStoredRootData(selectedRoot);
		verifyContent(selectedRootAfterDelete.getItems().get(0).getAttachments().get(0).getContent(),
				"testContent attachment");
		verifyContent(selectedRootAfterDelete.getItems().get(0).getAttachmentEntities().get(0).getContent(),
				"testContent attachmentEntity");
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void updateContentInDraft() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		var attachmentContentId = itemAttachment.getContentId();
		var attachmentEntityContentId = itemAttachmentEntity.getContentId();

		var newAttachmentContent = "new content attachment";
		putNewContentForAttachment(newAttachmentContent, selectedRoot.getItems().get(0).getId(), itemAttachment.getId());
		var newAttachmentEntityContent = "new content attachmentEntity";
		putNewContentForAttachmentEntity(newAttachmentEntityContent, itemAttachmentEntity.getId());

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterUpdate = selectStoredRootData(selectedRoot);
		verifyContent(selectedRootAfterUpdate.getItems().get(0).getAttachments().get(0).getContent(), newAttachmentContent);
		verifyContent(selectedRootAfterUpdate.getItems().get(0).getAttachmentEntities().get(0).getContent(),
				newAttachmentEntityContent);
		verifyEventContextEmptyForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
		verifyTwoUpdateEvents(newAttachmentContent, attachmentContentId, newAttachmentEntityContent,
				attachmentEntityContentId);
		var selectedRootAfterDeletion = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterDeletion.getItems().get(0).getAttachments().get(0).getContentId()).isNotEmpty();
		assertThat(selectedRootAfterDeletion.getItems().get(0).getAttachmentEntities().get(0).getContentId()).isNotEmpty();
	}

	@Test
	void contentCanBeReadForActiveRoot() throws Exception {
		var attachmentContent = "attachment Content";
		var attachmentEntityContent = "attachmentEntity Content";
		var selectedRoot = deepCreateAndActivate(attachmentContent, attachmentEntityContent);
		clearServiceHandlerContext();

		readAndValidateActiveContent(selectedRoot, attachmentContent, attachmentEntityContent);
	}

	@Test
	void noChangesOnAttachmentsContentStillAvailable() throws Exception {
		var attachmentContent = "attachment Content";
		var attachmentEntityContent = "attachmentEntity Content";
		var selectedRoot = deepCreateAndActivate(attachmentContent, attachmentEntityContent);
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var rootUrl = getRootUrl(selectedRoot.getId(), false);
		requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, "{\"title\":\"some other title\"}");

		prepareAndActiveDraft(rootUrl);
		verifyNoAttachmentEventsCalled();

		readAndValidateActiveContent(selectedRoot, attachmentContent, attachmentEntityContent);
	}

	@Test
	void deleteItemAndActivateDraft() throws Exception {
		var attachmentContent = "attachment Content";
		var attachmentEntityContent = "attachmentEntity Content";
		var selectedRoot = deepCreateAndActivate(attachmentContent, attachmentEntityContent);
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemUrl = getItemUrl(selectedRoot.getItems().get(0), false);
		requestHelper.executeDeleteWithMatcher(itemUrl, status().isNoContent());
		verifyNoAttachmentEventsCalled();

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterDelete = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterDelete.getItems()).isEmpty();
		verifyOnlyTwoDeleteEvents(selectedRoot.getItems().get(0).getAttachments().get(0).getContentId(),
				selectedRoot.getItems().get(0).getAttachmentEntities().get(0).getContentId());
	}

	@Test
	void deleteItemAndCancelDraft() throws Exception {
		var attachmentContent = "attachment Content";
		var attachmentEntityContent = "attachmentEntity Content";
		var selectedRoot = deepCreateAndActivate(attachmentContent, attachmentEntityContent);
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemUrl = getItemUrl(selectedRoot.getItems().get(0), false);
		requestHelper.executeDeleteWithMatcher(itemUrl, status().isNoContent());

		cancelDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterDelete = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterDelete.getItems()).isNotEmpty();
		assertThat(selectedRootAfterDelete.getItems().get(0).getAttachments()).isNotEmpty();
		assertThat(selectedRootAfterDelete.getItems().get(0).getAttachments().get(0).getContentId()).isNotEmpty();
		assertThat(selectedRootAfterDelete.getItems().get(0).getAttachmentEntities()).isNotEmpty();
		assertThat(selectedRootAfterDelete.getItems().get(0).getAttachmentEntities().get(0).getContentId()).isNotEmpty();
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void noEventsForForDeletedRoot() throws Exception {
		var selectedRoot = deepCreateAndActivate("attachmentContent", "attachmentEntityContent");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var rootUrl = getRootUrl(selectedRoot.getId(), true);
		requestHelper.executeDeleteWithMatcher(rootUrl, status().isNoContent());

		var draftPrepareUrl = rootUrl + "/TestDraftService.draftPrepare";
		requestHelper.executePostWithMatcher(draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isNotFound());

		var select = Select.from(TestDraftService_.DRAFT_ROOTS);
		var result = persistenceService.run(select).listOf(DraftRoots.class);
		assertThat(result).isEmpty();

		var attachmentContentId = selectedRoot.getItems().get(0).getAttachments().get(0).getContentId();
		var attachmentEntityContentId = selectedRoot.getItems().get(0).getAttachmentEntities().get(0).getContentId();

		verifyOnlyTwoDeleteEvents(attachmentContentId, attachmentEntityContentId);
	}

	@Test
	void errorInTransactionAfterCreateCallsDelete() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		createNewContentAndValidateEvents(selectedRoot);

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterCreate = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachments()).hasSize(2);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachmentEntities()).hasSize(2);
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void errorInTransactionAfterCreateCallsDeleteAndNothingForCancel() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		createNewContentAndValidateEvents(selectedRoot);

		cancelDraft(getRootUrl(selectedRoot.getId(), false));
		var selectedRootAfterCreate = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachments()).hasSize(1);
		assertThat(selectedRootAfterCreate.getItems().get(0).getAttachmentEntities()).hasSize(1);
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void errorInTransactionAfterUpdateCallsDelete() throws Exception {
		var attachmentContent = "testContent attachment";
		var attachmentEntityContent = "testContent attachmentEntity";
		var selectedRoot = deepCreateAndActivate(attachmentContent, attachmentEntityContent);
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		updateContentWithErrorAndValidateEvents(selectedRoot, itemAttachment, itemAttachmentEntity);

		testPersistenceHandler.reset();
		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		verifyNothingHasChangedInDraft(selectedRoot, attachmentContent, attachmentEntityContent);
	}

	@Test
	void errorInTransactionAfterUpdateCallsDeleteEvenIfDraftIsCancelled() throws Exception {
		var attachmentContent = "testContent attachment";
		var attachmentEntityContent = "testContent attachmentEntity";
		var selectedRoot = deepCreateAndActivate(attachmentContent, attachmentEntityContent);
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var itemAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		updateContentWithErrorAndValidateEvents(selectedRoot, itemAttachment, itemAttachmentEntity);

		testPersistenceHandler.reset();
		cancelDraft(getRootUrl(selectedRoot.getId(), false));
		verifyNothingHasChangedInDraft(selectedRoot, attachmentContent, attachmentEntityContent);
	}

	@Test
	void createAndDeleteAttachmentWorks() throws Exception {
		var selectedRoot = deepCreateAndActivate("testContent attachment", "testContent attachmentEntity");
		clearServiceHandlerContext();
		createNewDraftForExistingRoot(selectedRoot.getId());

		var itemAttachment = selectedRoot.getItems().get(0);

		var newAttachmentContent = "new attachment content";
		createAttachmentWithContent(newAttachmentContent, itemAttachment.getId());
		var newAttachmentEntityContent = "new attachmentEntity content";
		createAttachmentEntityWithContent(newAttachmentEntityContent, itemAttachment);

		var draftRoot = selectStoredRootData(DraftRoots_.CDS_NAME + "_drafts", selectedRoot);

		var existingAttachment = selectedRoot.getItems().get(0).getAttachments().get(0);
		var existingAttachmentEntity = selectedRoot.getItems().get(0).getAttachmentEntities().get(0);

		var newAttachment = draftRoot.getItems().get(0).getAttachments().stream().filter(
				attachment -> !attachment.getId().equals(existingAttachment.getId())).findAny().orElseThrow();
		var newAttachmentEntity = draftRoot.getItems().get(0).getAttachmentEntities().stream().filter(
				attachmentEntity -> !attachmentEntity.getId().equals(existingAttachmentEntity.getId())).findAny().orElseThrow();

		var attachmentDeleteUrl = getAttachmentBaseUrl(selectedRoot.getItems().get(0).getId(), newAttachment.getId(), false);
		var attachmentEntityDeleteUrl = getAttachmentEntityBaseUrl(newAttachmentEntity.getId(), false);

		requestHelper.executeDeleteWithMatcher(attachmentDeleteUrl, status().isNoContent());
		requestHelper.executeDeleteWithMatcher(attachmentEntityDeleteUrl, status().isNoContent());

		verifyTwoCreateAndDeleteEvents(newAttachmentContent, newAttachmentEntityContent);
		clearServiceHandlerContext();

		prepareAndActiveDraft(getRootUrl(selectedRoot.getId(), false));
		verifyNoAttachmentEventsCalled();
	}

	protected DraftRoots deepCreateAndActivate(String testContentAttachment,
			String testContentAttachmentEntity) throws Exception {
		var responseRoot = createNewDraft();
		var rootUrl = updateRoot(responseRoot);
		var responseItem = createItem(rootUrl);
		createAttachmentWithContent(testContentAttachment, responseItem.getId());
		createAttachmentEntityWithContent(testContentAttachmentEntity, responseItem);
		prepareAndActiveDraft(rootUrl);

		return selectStoredRootData(responseRoot);
	}

	private DraftRoots createNewDraft() throws Exception {
		var responseRootCdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(BASE_ROOT_URL, "{}");
		return Struct.access(responseRootCdsData).as(DraftRoots.class);
	}

	private void createNewDraftForExistingRoot(String rootId) throws Exception {
		var url = getRootUrl(rootId, true) + "/TestDraftService.draftEdit";
		requestHelper.executePostWithODataResponseAndAssertStatus(url, "{\"PreserveChanges\":true}", HttpStatus.OK);
	}

	private String updateRoot(DraftRoots responseRoot) throws Exception {
		responseRoot.setTitle("some title");
		var rootUrl = getRootUrl(responseRoot.getId(), false);
		requestHelper.executePatchWithODataResponseAndAssertStatusOk(rootUrl, responseRoot.toJson());
		return rootUrl;
	}

	private String getRootUrl(String rootId, boolean isActiveEntity) {
		return BASE_ROOT_URL + "(ID=" + rootId + ",IsActiveEntity=" + isActiveEntity + ")";
	}

	private Items createItem(String rootUrl) throws Exception {
		var item = Items.create();
		item.setTitle("some item");
		var itemUrl = rootUrl + "/items";
		var responseItemCdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(itemUrl, item.toJson());
		return Struct.access(responseItemCdsData).as(Items.class);
	}

	private void createAttachmentWithContent(String testContentAttachment, String itemId) throws Exception {
		createAttachmentWithContent(testContentAttachment, itemId, status().isNoContent(), false);
	}

	private void createAttachmentWithContent(String testContentAttachment, String itemId, ResultMatcher matcher,
			boolean withError) throws Exception {
		var responseAttachment = createAttachment(itemId);
		if (withError) {
			testPersistenceHandler.setThrowExceptionOnUpdate(true);
		}
		putNewContentForAttachment(testContentAttachment, itemId, responseAttachment.getId(), matcher);
	}

	private void putNewContentForAttachment(String testContentAttachment, String itemId,
			String attachmentId) throws Exception {
		putNewContentForAttachment(testContentAttachment, itemId, attachmentId, status().isNoContent());
	}

	private void putNewContentForAttachment(String testContentAttachment, String itemId, String attachmentId,
			ResultMatcher matcher) throws Exception {
		var attachmentPutUrl = getAttachmentBaseUrl(itemId, attachmentId, false) + "/content";
		requestHelper.setContentType("text/plain");
		requestHelper.executePutWithMatcher(attachmentPutUrl, testContentAttachment.getBytes(StandardCharsets.UTF_8),
				matcher);
		requestHelper.resetHelper();
	}

	private Attachments createAttachment(String itemId) throws Exception {
		var itemAttachment = Attachments.create();
		itemAttachment.setFileName("itemAttachment.txt");

		var attachmentPostUrl = BASE_URL + "Items(ID=" + itemId + ",IsActiveEntity=false)/attachments";
		var responseAttachmentCdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(attachmentPostUrl,
				itemAttachment.toJson());
		return Struct.access(responseAttachmentCdsData).as(Attachments.class);
	}

	private void createAttachmentEntityWithContent(String testContentAttachmentEntity,
			Items responseItem) throws Exception {
		createAttachmentEntityWithContent(testContentAttachmentEntity, responseItem, status().isNoContent(), false);
	}

	private void createAttachmentEntityWithContent(String testContentAttachmentEntity, Items responseItem,
			ResultMatcher matcher, boolean withError) throws Exception {
		var responseAttachmentEntity = createAttachmentEntity(responseItem);
		if (withError) {
			testPersistenceHandler.setThrowExceptionOnUpdate(true);
		}
		putNewContentForAttachmentEntity(testContentAttachmentEntity, responseAttachmentEntity.getId(), matcher);
	}

	private void putNewContentForAttachmentEntity(String testContentAttachmentEntity,
			String attachmentId) throws Exception {
		putNewContentForAttachmentEntity(testContentAttachmentEntity, attachmentId, status().isNoContent());
	}

	private void putNewContentForAttachmentEntity(String testContentAttachmentEntity, String attachmentId,
			ResultMatcher matcher) throws Exception {
		var attachmentEntityPutUrl = BASE_URL + "/AttachmentEntity(ID=" + attachmentId + ",IsActiveEntity=false)/content";
		requestHelper.setContentType("image/jpeg");
		requestHelper.executePutWithMatcher(attachmentEntityPutUrl,
				testContentAttachmentEntity.getBytes(StandardCharsets.UTF_8), matcher);
		requestHelper.resetHelper();
	}

	private AttachmentEntity createAttachmentEntity(Items responseItem) throws Exception {
		var itemAttachmentEntity = AttachmentEntity.create();
		itemAttachmentEntity.setFileName("itemAttachmentEntity.txt");

		var attachmentEntityPostUrl = getItemUrl(responseItem, false) + "/attachmentEntities";
		var responseAttachmentEntityCdsData = requestHelper.executePostWithODataResponseAndAssertStatusCreated(
				attachmentEntityPostUrl, itemAttachmentEntity.toJson());
		return Struct.access(responseAttachmentEntityCdsData).as(AttachmentEntity.class);
	}

	private String getItemUrl(Items responseItem, boolean isActiveEntity) {
		return BASE_URL + "Items(ID=" + responseItem.getId() + ",IsActiveEntity=" + isActiveEntity + ")";
	}

	protected String getAttachmentBaseUrl(String itemId, String attachmentId, boolean isActiveEntity) {
		return BASE_URL + "Items_attachments(up__ID=" + itemId + ",ID=" + attachmentId + ",IsActiveEntity=" + isActiveEntity + ")";
	}

	protected String getAttachmentEntityBaseUrl(String attachmentId, boolean isActiveEntity) {
		return BASE_URL + "AttachmentEntity(ID=" + attachmentId + ",IsActiveEntity=" + isActiveEntity + ")";
	}

	private void prepareAndActiveDraft(String rootUrl) throws Exception {
		var draftPrepareUrl = rootUrl + "/TestDraftService.draftPrepare";
		var draftActivateUrl = rootUrl + "/TestDraftService.draftActivate";
		requestHelper.executePostWithMatcher(draftPrepareUrl, "{\"SideEffectsQualifier\":\"\"}", status().isOk());
		requestHelper.executePostWithMatcher(draftActivateUrl, "{}", status().isOk());
	}

	private void cancelDraft(String rootUrl) throws Exception {
		requestHelper.executeDeleteWithMatcher(rootUrl, status().isNoContent());
	}

	private DraftRoots selectStoredRootData(DraftRoots responseRoot) {
		return selectStoredRootData(DraftRoots_.CDS_NAME, responseRoot);
	}

	private DraftRoots selectStoredRootData(String entityName, DraftRoots responseRoot) {
		var select = Select.from(entityName).where(root -> root.get(DraftRoots.ID).eq(responseRoot.getId())).columns(
				StructuredType::_all, root -> root.to(DraftRoots.ITEMS)
						.expand(StructuredType::_all, item -> item.to(Items.ATTACHMENTS).expand(),
								item -> item.to(Items.ATTACHMENT_ENTITIES).expand()));
		return persistenceService.run(select).single(DraftRoots.class);
	}

	protected void readAndValidateActiveContent(DraftRoots selectedRoot, String attachmentContent,
			String attachmentEntityContent) throws Exception {
		var attachmentUrl = getAttachmentBaseUrl(selectedRoot.getItems().get(0).getId(), selectedRoot.getItems().get(0)
				.getAttachments().get(0).getId(),	true) + "/content";
		var attachmentEntityUrl = getAttachmentEntityBaseUrl(selectedRoot.getItems().get(0).getAttachmentEntities().get(0)
				.getId(), true) + "/content";

		Awaitility.await().atMost(40, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
			var attachmentResponse = requestHelper.executeGet(attachmentUrl);
			var attachmentEntityResponse = requestHelper.executeGet(attachmentEntityUrl);
			var attachmentContentAsString = attachmentResponse.getResponse().getContentAsString();
			var attachmentEntityContentAsString = attachmentEntityResponse.getResponse().getContentAsString();

			var booleanResult = attachmentContentAsString.equals(attachmentContent) && attachmentEntityContentAsString.equals(
					attachmentEntityContent);

			if (!booleanResult) {
				logger.info(
						"Attachment response content: {}, Attachment Test Content: {}, Attachment Entity response content: {}, Attachment Entity Test Content: {}",
						attachmentContentAsString, attachmentContent, attachmentEntityContentAsString, attachmentEntityContent);
			}
			return booleanResult;
		});
		clearServiceHandlerContext();

		var attachmentResponse = requestHelper.executeGet(attachmentUrl);
		var attachmentEntityResponse = requestHelper.executeGet(attachmentEntityUrl);

		assertThat(attachmentResponse.getResponse().getContentAsString()).isEqualTo(attachmentContent);
		assertThat(attachmentEntityResponse.getResponse().getContentAsString()).isEqualTo(attachmentEntityContent);
		verifyTwoReadEvents();
	}

	private void deleteContent(DraftRoots selectedRoot, Attachments itemAttachment,
			AttachmentEntity itemAttachmentEntity) throws Exception {
		var attachmentUrl = getAttachmentBaseUrl(selectedRoot.getItems().get(0).getId(), itemAttachment.getId(),
				false) + "/content";
		var attachmentEntityUrl = getAttachmentEntityBaseUrl(itemAttachmentEntity.getId(), false) + "/content";

		requestHelper.executeDeleteWithMatcher(attachmentUrl, status().isNoContent());
		requestHelper.executeDeleteWithMatcher(attachmentEntityUrl, status().isNoContent());
	}

	private void updateFileName(DraftRoots selectedRoot, Attachments itemAttachment, AttachmentEntity itemAttachmentEntity,
			String changedAttachmentFileName, String changedAttachmentEntityFileName) throws Exception {
		updateFileName(selectedRoot, itemAttachment, itemAttachmentEntity, changedAttachmentFileName,
				changedAttachmentEntityFileName, HttpStatus.OK);
	}

	private void updateFileName(DraftRoots selectedRoot, Attachments itemAttachment, AttachmentEntity itemAttachmentEntity,
			String changedAttachmentFileName, String changedAttachmentEntityFileName, HttpStatus httpStatus) throws Exception {
		var attachmentUrl = getAttachmentBaseUrl(selectedRoot.getItems().get(0).getId(), itemAttachment.getId(), false);
		var attachmentEntityUrl = getAttachmentEntityBaseUrl(itemAttachmentEntity.getId(), false);

		requestHelper.executePatchWithODataResponseAndAssertStatus(attachmentUrl,
				"{\"fileName\":\"" + changedAttachmentFileName + "\"}", httpStatus);
		requestHelper.executePatchWithODataResponseAndAssertStatus(attachmentEntityUrl,
				"{\"fileName\":\"" + changedAttachmentEntityFileName + "\"}", httpStatus);
	}

	private void updateContentWithErrorAndValidateEvents(DraftRoots selectedRoot, Attachments itemAttachment,
			AttachmentEntity itemAttachmentEntity) throws Exception {
		testPersistenceHandler.setThrowExceptionOnUpdate(true);
		var newAttachmentContent = "new content attachment";
		putNewContentForAttachment(newAttachmentContent, selectedRoot.getItems().get(0).getId(), itemAttachment.getId(),
				status().is5xxServerError());
		var newAttachmentEntityContent = "new content attachmentEntity";
		putNewContentForAttachmentEntity(newAttachmentEntityContent, itemAttachmentEntity.getId(),
				status().is5xxServerError());
		verifyTwoCreateAndRevertedDeleteEvents();
		clearServiceHandlerContext();
	}

	private void verifyNothingHasChangedInDraft(DraftRoots selectedRoot, String attachmentContent,
			String attachmentEntityContent) throws IOException {
		var selectedRootAfterUpdate = selectStoredRootData(selectedRoot);

		verifyContent(selectedRootAfterUpdate.getItems().get(0).getAttachments().get(0).getContent(), attachmentContent);
		verifyContent(selectedRootAfterUpdate.getItems().get(0).getAttachmentEntities().get(0).getContent(),
				attachmentEntityContent);
		verifyNoAttachmentEventsCalled();
		var selectedRootAfterDeletion = selectStoredRootData(selectedRoot);
		assertThat(selectedRootAfterDeletion.getItems().get(0).getAttachments().get(0).getContentId()).isNotEmpty();
		assertThat(selectedRootAfterDeletion.getItems().get(0).getAttachmentEntities().get(0).getContentId()).isNotEmpty();
	}

	private void createNewContentAndValidateEvents(DraftRoots selectedRoot) throws Exception {
		var itemAttachment = selectedRoot.getItems().get(0);
		var newAttachmentContent = "new attachment content";
		createAttachmentWithContent(newAttachmentContent, itemAttachment.getId(), status().is5xxServerError(), true);
		testPersistenceHandler.reset();
		var newAttachmentEntityContent = "new attachmentEntity content";
		createAttachmentEntityWithContent(newAttachmentEntityContent, itemAttachment, status().is5xxServerError(), true);
		verifyTwoCreateAndDeleteEvents(newAttachmentContent, newAttachmentEntityContent);
		clearServiceHandlerContext();
		testPersistenceHandler.reset();
	}

	private String getResponseContent(MvcResult attachmentResponse) throws UnsupportedEncodingException {
		return attachmentResponse.getResponse().getStatus() == HttpStatus.OK.value() ? attachmentResponse.getResponse()
				.getContentAsString() : "";
	}

	protected abstract void verifyContentId(String contentId, String attachmentId);

	protected abstract void verifyContent(InputStream attachment, String testContent) throws IOException;

	protected abstract void verifyNoAttachmentEventsCalled();

	protected abstract void clearServiceHandlerContext();

	protected abstract void verifyEventContextEmptyForEvent(String... events);

	protected abstract void verifyOnlyTwoCreateEvents(String newAttachmentContent, String newAttachmentEntityContent);

	protected abstract void verifyTwoCreateAndDeleteEvents(String newAttachmentContent, String newAttachmentEntityContent);

	protected abstract void verifyTwoReadEvents();

	protected abstract void verifyOnlyTwoDeleteEvents(String attachmentContentId, String attachmentEntityContentId);

	protected abstract void verifyTwoUpdateEvents(String newAttachmentContent, String attachmentContentId,
			String newAttachmentEntityContent, String attachmentEntityContentId);

	protected abstract void verifyTwoCreateAndRevertedDeleteEvents();

}

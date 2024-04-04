package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.common.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.ItemEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.RootEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPersistenceHandler;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

@SpringBootTest
@AutoConfigureMockMvc
abstract class OdataRequestValidationBase {

	@Autowired(required = false)
	protected TestPluginAttachmentsServiceHandler serviceHandler;
	@Autowired
	private MockHttpRequestHelper requestHelper;
	@Autowired
	private PersistenceService persistenceService;
	@Autowired
	private TableDataDeleter dataDeleter;
	@Autowired
	private TestPersistenceHandler testPersistenceHandler;

	@AfterEach
	void teardown() {
		dataDeleter.deleteData(Roots_.CDS_NAME);
		clearServiceHandlerContext();
		clearServiceHandlerDocuments();
		requestHelper.resetHelper();
		testPersistenceHandler.reset();
	}

	@Test
	void deepCreateWorks() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		verifySelectedRoot(selectedRoot, serviceRoot);
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void putContentWorksForUrlsWithNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		var content = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		var attachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);

		verifyContentAndDocumentId(attachment, content, itemAttachment);
		verifySingleCreateEvent(attachment.getDocumentId(), content);
	}

	@Test
	void putContentWorksForUrlsWithoutNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);

		var content = putContentForAttachmentWithoutNavigation(itemAttachment);
		var attachment = selectUpdatedAttachment(itemAttachment);

		verifyContentAndDocumentIdForAttachmentEntity(attachment, content, itemAttachment);
		verifySingleCreateEvent(attachment.getDocumentId(), content);
	}

	@Test
	void expandReadOfAttachmentsHasNoFilledContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);

		var url = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSameSizeAs(item.getAttachments());
		assertThat(responseItem.getAttachments()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getDocumentId()).isNull();
		});
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void navigationReadOfAttachmentsHasFilledContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		var content = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);

		var url = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSameSizeAs(item.getAttachments());

		var attachmentWithExpectedContent = responseItem.getAttachments().stream()
																																								.filter(attach -> attach.getId().equals(itemAttachment.getId())).findAny()
																																								.orElseThrow();
		assertThat(attachmentWithExpectedContent).containsEntry("content@mediaContentType", "application/octet-stream;charset=UTF-8")
				.containsEntry(Attachments.FILE_NAME, itemAttachment.getFileName());
		verifyDocumentId(attachmentWithExpectedContent, itemAttachment.getId(), itemAttachment.getDocumentId());
		verifySingleCreateEvent(attachmentWithExpectedContent.getDocumentId(), content);
	}

	@Test
	void navigationReadOfAttachmentsReturnsContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		var content = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachment(selectedItemAfterChange);

		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId()) + "/content";
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
		verifySingleReadEvent(itemAttachmentAfterChange.getDocumentId());
	}

	@Test
	void navigationDeleteOfContentClears() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachment(selectedItemAfterChange);

		executeDeleteAndCheckNoDataCanBeRead(buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId()), itemAttachmentAfterChange.getDocumentId());

		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSameSizeAs(item.getAttachments());
		assertThat(responseItem.getAttachments()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getDocumentId()).isNull();
		});
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void navigationDeleteOfAttachmentClearsContentField() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachment(selectedItemAfterChange);

		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId());
		requestHelper.executeDelete(url);
		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSize(1);
		assertThat(responseItem.getAttachments()).first().satisfies(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getDocumentId()).isNull();
		});
		verifySingleDeletionEvent(itemAttachmentAfterChange.getDocumentId());
	}

	@Test
	void navigationDeleteCallsTwiceReturnsError() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachment(selectedItemAfterChange);

		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId());
		requestHelper.executeDelete(url);
		var result = requestHelper.executeDelete(url);

		assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		verifySingleDeletionEvent(itemAttachmentAfterChange.getDocumentId());
	}

	@Test
	void directReadOfAttachmentsHasNoContentFilled() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		var responseAttachment = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Attachments.class, HttpStatus.OK);

		assertThat(responseAttachment.get("content@mediaContentType")).isNull();
		assertThat(responseAttachment.getDocumentId()).isNull();
		assertThat(responseAttachment.getFileName()).isEqualTo(itemAttachment.getFileName());
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void directReadOfAttachmentsHasFilledContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);
		clearServiceHandlerContext();

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		var responseAttachment = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Attachments.class, HttpStatus.OK);

		assertThat(responseAttachment).containsEntry("content@mediaContentType", "application/octet-stream;charset=UTF-8")
				.containsEntry(Attachments.FILE_NAME, itemAttachment.getFileName());
		verifyDocumentId(responseAttachment, itemAttachment.getId(), itemAttachment.getDocumentId());
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void directReadOfAttachmentsReturnsContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		var content = putContentForAttachmentWithoutNavigation(itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachmentEntity(selectedItemAfterChange);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId()) + "/content";
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
		verifySingleReadEvent(itemAttachmentAfterChange.getDocumentId());
	}

	@Test
	void directDeleteOfContentClears() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachmentEntity(selectedItemAfterChange);

		executeDeleteAndCheckNoDataCanBeRead(buildDirectAttachmentEntityUrl(itemAttachment.getId()), itemAttachmentAfterChange.getDocumentId());

		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachmentEntities()).hasSameSizeAs(item.getAttachmentEntities());
		assertThat(responseItem.getAttachmentEntities()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getDocumentId()).isNull();
		});
		verifyNoAttachmentEventsCalled();
	}

	@Test
	void directDeleteOfAttachmentClearsContentField() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentAfterChange = getRandomItemAttachmentEntity(selectedItemAfterChange);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		requestHelper.executeDelete(url);
		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachmentEntities()).isEmpty();
		verifySingleDeletionEvent(itemAttachmentAfterChange.getDocumentId());
	}

	@Test
	void directDeleteCalledTwiceReturnsError() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);
		clearServiceHandlerContext();

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		requestHelper.executeDelete(url);
		MvcResult mvcResult = requestHelper.executeDelete(url);

		assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		if (Objects.nonNull(serviceHandler)) {
			Awaitility.await().until(() -> serviceHandler.getEventContext().size() == 1);
			verifyNumberOfEvents(AttachmentService.EVENT_MARK_AS_DELETED, 1);
			verifyEventContextEmptyForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT, AttachmentService.EVENT_READ_ATTACHMENT);
		}
	}

	@Test
	void rootDeleteDeletesAllContents() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);
		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachmentEntity = getRandomItemAttachmentEntity(item);
		var itemAttachment = getRandomItemAttachment(item);

		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		putContentForAttachmentWithoutNavigation(itemAttachmentEntity);
		verifyNumberOfEvents(AttachmentService.EVENT_CREATE_ATTACHMENT, 2);
		clearServiceHandlerContext();
		var selectedItemAfterChange = selectItem(item);
		var itemAttachmentEntityAfterChange = getRandomItemAttachmentEntity(selectedItemAfterChange);
		var itemAttachmentAfterChange = getRandomItemAttachment(selectedItemAfterChange);

		var url = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots(" + selectedRoot.getId() + ")";
		requestHelper.executeDeleteWithMatcher(url, status().isNoContent());

		verifyTwoDeleteEvents(itemAttachmentEntityAfterChange, itemAttachmentAfterChange);
	}

	@Test
	void updateContentWorksForUrlsWithNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		itemAttachment.setNote("note 1");
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		itemAttachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);
		itemAttachment.setNote("note 2");
		var content = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		var attachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);

		verifyContentAndDocumentId(attachment, content, itemAttachment);
		verifySingleCreateAndUpdateEvent(attachment.getDocumentId(), itemAttachment.getDocumentId(), content);
	}

	@Test
	void updateContentWorksForUrlsWithoutNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		itemAttachment.setNote("note 1");
		putContentForAttachmentWithoutNavigation(itemAttachment);
		itemAttachment = selectUpdatedAttachment(itemAttachment);
		itemAttachment.setNote("note 2");
		var content = putContentForAttachmentWithoutNavigation(itemAttachment);
		var attachment = selectUpdatedAttachment(itemAttachment);

		verifyContentAndDocumentIdForAttachmentEntity(attachment, content, itemAttachment);
		verifySingleCreateAndUpdateEvent(attachment.getDocumentId(), itemAttachment.getDocumentId(), content);
	}

	@Test
	void errorInTransactionAfterCreateCallsDelete() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		testPersistenceHandler.setThrowExceptionOnUpdate(true);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment, status().is5xxServerError());
		var attachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);

		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getDocumentId());
		assertThat(attachment.getContent()).isEqualTo(itemAttachment.getContent());
	}

	@Test
	void updateContentWithErrorsResetsForUrlsWithNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		itemAttachment.setNote("note 1");
		var content = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		itemAttachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);
		itemAttachment.setNote("note 2");
		testPersistenceHandler.setThrowExceptionOnUpdate(true);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment, status().is5xxServerError());
		var attachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);

		verifyContentAndDocumentId(attachment, content, itemAttachment);
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getDocumentId());
		verifySingleCreateAndUpdateEvent(attachment.getDocumentId(), itemAttachment.getDocumentId(), content);
	}

	@Test
	void updateContentWithErrorResetsForUrlsWithoutNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		itemAttachment.setNote("note 1");
		var content = putContentForAttachmentWithoutNavigation(itemAttachment);
		itemAttachment = selectUpdatedAttachment(itemAttachment);
		itemAttachment.setNote("note 2");
		testPersistenceHandler.setThrowExceptionOnUpdate(true);
		putContentForAttachmentWithoutNavigation(itemAttachment, status().is5xxServerError());
		var attachment = selectUpdatedAttachment(itemAttachment);

		verifyContentAndDocumentIdForAttachmentEntity(attachment, content, itemAttachment);
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getDocumentId());
		verifySingleCreateAndUpdateEvent(attachment.getDocumentId(), itemAttachment.getDocumentId(), content);
	}

	private Items selectItem(Items item) {
		var selectedRootAfterContentCreated = selectStoredRootWithDeepData();
		return selectedRootAfterContentCreated.getItems().stream().filter(i -> i.getId().equals(item.getId())).findAny()
											.orElseThrow();
	}

	private Roots buildServiceRootWithDeepData() {
		return RootEntityBuilder.create().setTitle("some root title")
											.addAttachments(AttachmentsEntityBuilder.create().setFileName("fileRoot.txt").setMimeType("text/plain"))
											.addItems(ItemEntityBuilder.create().setTitle("some item 1 title")
																							.addAttachments(AttachmentsBuilder.create().setFileName("fileItem1.txt")
																																									.setMimeType("text/plain"), AttachmentsBuilder.create()
																																																																							.setFileName("fileItem2.txt")
																																																																							.setMimeType("text/plain")), ItemEntityBuilder.create()
																																																																																																						.setTitle("some item 2 title")
																																																																																																						.addAttachmentEntities(AttachmentsEntityBuilder.create()
																																																																																																																															.setFileName("fileItem3.text")
																																																																																																																															.setMimeType("text/plain"))
																																																																																																						.addAttachments(AttachmentsBuilder.create()
																																																																																																																								.setFileName("fileItem3.text")
																																																																																																																								.setMimeType("text/plain")))
											.build();
	}

	private void postServiceRoot(Roots serviceRoot) throws Exception {
		var url = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots";
		requestHelper.executePostWithMatcher(url, serviceRoot.toJson(), status().isCreated());
	}

	private Roots selectStoredRootWithDeepData() {
		CqnSelect select = Select.from(Roots_.class)
																							.columns(StructuredType::_all, root -> root.attachments().expand(), root -> root.items()
																																																																																																					.expand(StructuredType::_all, item -> item.attachments()
																																																																																																																																													.expand(), item -> item.attachmentEntities()
																																																																																																																																																																		.expand()));
		var result = persistenceService.run(select);
		return result.single(Roots.class);
	}

	private void verifySelectedRoot(Roots selectedRoot, Roots serviceRoot) {
		assertThat(selectedRoot.getId()).isNotEmpty();
		assertThat(selectedRoot.getTitle()).isEqualTo(serviceRoot.getTitle());
		assertThat(selectedRoot.getAttachments()).hasSize(1).first().satisfies(attachment -> {
			assertThat(attachment.getId()).isNotEmpty();
			assertThat(attachment.getFileName()).isEqualTo(serviceRoot.getAttachments().get(0).getFileName());
			assertThat(attachment.getMimeType()).isEqualTo(serviceRoot.getAttachments().get(0).getMimeType());
		});
		assertThat(selectedRoot.getItems()).hasSize(2).first().satisfies(item -> {
			assertThat(item.getId()).isNotEmpty();
			assertThat(item.getTitle()).isEqualTo(serviceRoot.getItems().get(0).getTitle());
			assertThat(item.getAttachments()).hasSize(2);
		});
		assertThat(selectedRoot.getItems().get(1).getId()).isNotEmpty();
		assertThat(selectedRoot.getItems().get(1).getTitle()).isEqualTo(serviceRoot.getItems().get(1).getTitle());
		assertThat(selectedRoot.getItems().get(1).getAttachments()).hasSize(1);
	}

	private Attachments getRandomItemAttachment(Items selectedItem) {
		return selectedItem.getAttachments().get(0);
	}

	private AttachmentEntity getRandomItemAttachmentEntity(Items selectedItem) {
		return selectedItem.getAttachmentEntities().get(0);
	}

	private Items getItemWithAttachment(Roots selectedRoot) {
		return selectedRoot.getItems().stream().filter(item -> !item.getAttachments().isEmpty()).findAny().orElseThrow();
	}

	private Items getItemWithAttachmentEntity(Roots selectedRoot) {
		return selectedRoot.getItems().stream().filter(item -> !item.getAttachmentEntities().isEmpty()).findAny()
											.orElseThrow();
	}

	private String putContentForAttachmentWithNavigation(Roots selectedRoot, Attachments itemAttachment) throws Exception {
		return putContentForAttachmentWithNavigation(selectedRoot, itemAttachment, status().isNoContent());
	}

	private String putContentForAttachmentWithNavigation(Roots selectedRoot, Attachments itemAttachment, ResultMatcher matcher) throws Exception {
		var selectedItem = selectedRoot.getItems().stream().filter(item -> item.getAttachments().stream()
																																																																							.anyMatch(attach -> attach.getId()
																																																																																													.equals(itemAttachment.getId())))
																							.findAny().orElseThrow();
		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), selectedItem.getId(), itemAttachment.getId()) + "/content";

		var testContent = "testContent" + itemAttachment.getNote();
		requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		requestHelper.executePutWithMatcher(url, testContent.getBytes(StandardCharsets.UTF_8), matcher);
		return testContent;
	}

	private String buildNavigationAttachmentUrl(String rootId, String itemId, String attachmentId) {
		return "/odata/v4/TestService/Roots(" + rootId + ")/items(" + itemId + ")" + "/attachments(ID=" + attachmentId + ",up__ID=" + itemId + ")";
	}

	private String buildExpandAttachmentUrl(String rootId, String itemId) {
		return "/odata/v4/TestService/Roots(" + rootId + ")/items(" + itemId + ")" + "?$expand=attachments,attachmentEntities";
	}

	private String putContentForAttachmentWithoutNavigation(AttachmentEntity itemAttachment) throws Exception {
		return putContentForAttachmentWithoutNavigation(itemAttachment, status().isNoContent());
	}

	private String putContentForAttachmentWithoutNavigation(AttachmentEntity itemAttachment, ResultMatcher matcher) throws Exception {
		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId()) + "/content";
		var testContent = "testContent" + itemAttachment.getNote();
		requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		requestHelper.executePutWithMatcher(url, testContent.getBytes(StandardCharsets.UTF_8), matcher);
		return testContent;
	}

	private String buildDirectAttachmentEntityUrl(String attachmentId) {
		return MockHttpRequestHelper.ODATA_BASE_URL + "TestService/AttachmentEntity(" + attachmentId + ")";
	}

	private Attachments selectUpdatedAttachmentWithExpand(Roots selectedRoot, Attachments itemAttachment) {
		CqnSelect attachmentSelect = Select.from(Items_.class).where(a -> a.ID().eq(selectedRoot.getItems().get(0).getId()))
																																	.columns(item -> item.attachments().expand());
		var result = persistenceService.run(attachmentSelect);
		var items = result.single(Items.class);
		return items.getAttachments().stream().filter(attach -> itemAttachment.getId().equals(attach.getId())).findAny()
											.orElseThrow();
	}

	private AttachmentEntity selectUpdatedAttachment(AttachmentEntity itemAttachment) {
		CqnSelect attachmentSelect = Select.from(AttachmentEntity_.class).where(a -> a.ID().eq(itemAttachment.getId()));
		var result = persistenceService.run(attachmentSelect);
		return result.single(AttachmentEntity.class);
	}

	private void executeDeleteAndCheckNoDataCanBeRead(String baseUrl, String documentId) throws Exception {
		var url = baseUrl + "/content";
		requestHelper.executeDelete(url);
		verifySingleDeletionEvent(documentId);
		clearServiceHandlerContext();
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentLength()).isZero();
		assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	protected abstract void verifyTwoDeleteEvents(AttachmentEntity itemAttachmentEntityAfterChange, Attachments itemAttachmentAfterChange);

	protected abstract void verifyNumberOfEvents(String event, int number);

	protected abstract void verifyDocumentId(Attachments attachmentWithExpectedContent, String attachmentId, String documentId);

	protected abstract void verifyContentAndDocumentId(Attachments attachment, String testContent, Attachments itemAttachment) throws IOException;

	protected abstract void verifyContentAndDocumentIdForAttachmentEntity(AttachmentEntity attachment, String testContent, AttachmentEntity itemAttachment) throws IOException;

	protected abstract void clearServiceHandlerContext();

	protected abstract void clearServiceHandlerDocuments();

	protected abstract void verifySingleCreateEvent(String documentId, String content);

	protected abstract void verifySingleCreateAndUpdateEvent(String resultDocumentId, String toBeDeletedDocumentId, String content);

	protected abstract void verifySingleDeletionEvent(String documentId);

	protected abstract void verifySingleReadEvent(String documentId);

	protected abstract void verifyNoAttachmentEventsCalled();

	protected abstract void verifyEventContextEmptyForEvent(String... events);

}

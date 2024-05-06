package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;
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

	protected static final Logger logger = LoggerFactory.getLogger(OdataRequestValidationBase.class);

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

		verifyContentAndContentId(attachment, content, itemAttachment);
		verifySingleCreateEvent(attachment.getContentId(), content);
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

		verifyContentAndContentIdForAttachmentEntity(attachment, content, itemAttachment);
		verifySingleCreateEvent(attachment.getContentId(), content);
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
			assertThat(attachment.getContentId()).isNull();
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

		var attachmentWithExpectedContent = responseItem.getAttachments().stream().filter(
				attach -> attach.getId().equals(itemAttachment.getId())).findAny().orElseThrow();
		assertThat(attachmentWithExpectedContent).containsEntry("content@mediaContentType",
				"application/octet-stream;charset=UTF-8").containsEntry(Attachments.FILE_NAME, itemAttachment.getFileName());
		assertThat(attachmentWithExpectedContent.getStatus()).isNotEmpty();
		verifyContentId(attachmentWithExpectedContent, itemAttachment.getId(), itemAttachment.getContentId());
		verifySingleCreateEvent(attachmentWithExpectedContent.getContentId(), content);
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
		executeContentRequestAndValidateContent(url, content);
		verifySingleReadEvent(itemAttachmentAfterChange.getContentId());
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

		executeDeleteAndCheckNoDataCanBeRead(
				buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId()),
				itemAttachmentAfterChange.getContentId());

		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class,
				HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSameSizeAs(item.getAttachments());
		assertThat(responseItem.getAttachments()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getContentId()).isNull();
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
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class,
				HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSize(1);
		assertThat(responseItem.getAttachments()).first().satisfies(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getContentId()).isNull();
		});
		verifySingleDeletionEvent(itemAttachmentAfterChange.getContentId());
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

		assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
		verifySingleDeletionEvent(itemAttachmentAfterChange.getContentId());
	}

	@Test
	void directReadOfAttachmentsHasNoContentFilled() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		var responseAttachment = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Attachments.class,
				HttpStatus.OK);

		assertThat(responseAttachment.get("content@mediaContentType")).isNull();
		assertThat(responseAttachment.getContentId()).isNull();
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
		var responseAttachment = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Attachments.class,
				HttpStatus.OK);

		assertThat(responseAttachment).containsEntry("content@mediaContentType", "application/octet-stream;charset=UTF-8")
				.containsEntry(Attachments.FILE_NAME, itemAttachment.getFileName());
		verifyContentId(responseAttachment, itemAttachment.getId(), itemAttachment.getContentId());
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
		executeContentRequestAndValidateContent(url, content);
		verifySingleReadEvent(itemAttachmentAfterChange.getContentId());
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

		executeDeleteAndCheckNoDataCanBeRead(buildDirectAttachmentEntityUrl(itemAttachment.getId()),
				itemAttachmentAfterChange.getContentId());

		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class,
				HttpStatus.OK);

		assertThat(responseItem.getAttachmentEntities()).hasSameSizeAs(item.getAttachmentEntities());
		assertThat(responseItem.getAttachmentEntities()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getContentId()).isNull();
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
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class,
				HttpStatus.OK);

		assertThat(responseItem.getAttachmentEntities()).isEmpty();
		verifySingleDeletionEvent(itemAttachmentAfterChange.getContentId());
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

		assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
		if (Objects.nonNull(serviceHandler)) {
			Awaitility.await().until(() -> serviceHandler.getEventContext().size() == 1);
			verifyNumberOfEvents(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED, 1);
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

		verifyContentAndContentId(attachment, content, itemAttachment);
		verifySingleCreateAndUpdateEvent(attachment.getContentId(), itemAttachment.getContentId(), content);
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

		verifyContentAndContentIdForAttachmentEntity(attachment, content, itemAttachment);
		verifySingleCreateAndUpdateEvent(attachment.getContentId(), itemAttachment.getContentId(), content);
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

		assertThat(attachment.getContentId()).isEqualTo(itemAttachment.getContentId());
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

		verifyContentAndContentId(attachment, content, itemAttachment);
		assertThat(attachment.getContentId()).isEqualTo(itemAttachment.getContentId());
		verifySingleCreateAndUpdateEvent(attachment.getContentId(), itemAttachment.getContentId(), content);
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

		verifyContentAndContentIdForAttachmentEntity(attachment, content, itemAttachment);
		assertThat(attachment.getContentId()).isEqualTo(itemAttachment.getContentId());
		verifySingleCreateAndUpdateEvent(attachment.getContentId(), itemAttachment.getContentId(), content);
	}

	@ParameterizedTest
	@CsvSource({"status,INFECTED", "contentId,TEST"})
	void statusCannotBeUpdated(String field, String value) throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);
		itemAttachment.setStatus(value);
		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());

		requestHelper.resetHelper();
		requestHelper.executePatchWithODataResponseAndAssertStatus(url, "{\"" + field + "\":\"" + value + "\"}",
				HttpStatus.OK);

		selectedRoot = selectStoredRootWithDeepData();
		item = getItemWithAttachmentEntity(selectedRoot);
		itemAttachment = getRandomItemAttachmentEntity(item);
		assertThat(itemAttachment.get(field)).isNotNull().isNotEqualTo(value);
	}

	@Test
	void wrongEtagCouldNotBeUpdated() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		requestHelper.executePatchWithODataResponseAndAssertStatus(url, "{\"fileName\":\"test_for_change.txt\"}",
				"W/\"2024-05-06T15:24:29.657713600Z\"",	HttpStatus.PRECONDITION_FAILED);

		var selectedRootAfterChange = selectStoredRootWithDeepData();
		var itemAfterChange = getItemWithAttachmentEntity(selectedRootAfterChange);
		var itemAttachmentAfterChange = getRandomItemAttachmentEntity(itemAfterChange);
		assertThat(itemAttachmentAfterChange.getFileName()).isEqualTo(itemAttachment.getFileName());
	}

	protected Items selectItem(Items item) {
		var selectedRootAfterContentCreated = selectStoredRootWithDeepData();
		return selectedRootAfterContentCreated.getItems().stream().filter(i -> i.getId().equals(item.getId())).findAny()
											.orElseThrow();
	}

	protected Roots buildServiceRootWithDeepData() {
		return RootEntityBuilder.create().setTitle("some root title").addAttachments(
				AttachmentsEntityBuilder.create().setFileName("fileRoot.txt").setMimeType("text/plain")).addItems(
				ItemEntityBuilder.create().setTitle("some item 1 title")
						.addAttachments(AttachmentsBuilder.create().setFileName("fileItem1.txt").setMimeType("text/plain"),
								AttachmentsBuilder.create().setFileName("fileItem2.txt").setMimeType("text/plain")),
				ItemEntityBuilder.create().setTitle("some item 2 title")
						.addAttachmentEntities(AttachmentsEntityBuilder.create().setFileName("fileItem3.text").setMimeType("text/plain"))
						.addAttachments(AttachmentsBuilder.create().setFileName("fileItem3.text").setMimeType("text/plain"))).build();
	}

	protected void postServiceRoot(Roots serviceRoot) throws Exception {
		var url = MockHttpRequestHelper.ODATA_BASE_URL + "TestService/Roots";
		requestHelper.executePostWithMatcher(url, serviceRoot.toJson(), status().isCreated());
	}

	protected Roots selectStoredRootWithDeepData() {
		CqnSelect select = Select.from(Roots_.class).columns(StructuredType::_all, root -> root.attachments().expand(),
				root -> root.items().expand(StructuredType::_all, item -> item.attachments().expand(),
						item -> item.attachmentEntities().expand()));
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

	protected Attachments getRandomItemAttachment(Items selectedItem) {
		return selectedItem.getAttachments().get(0);
	}

	private AttachmentEntity getRandomItemAttachmentEntity(Items selectedItem) {
		return selectedItem.getAttachmentEntities().get(0);
	}

	protected Items getItemWithAttachment(Roots selectedRoot) {
		return selectedRoot.getItems().stream().filter(item -> !item.getAttachments().isEmpty()).findAny().orElseThrow();
	}

	private Items getItemWithAttachmentEntity(Roots selectedRoot) {
		return selectedRoot.getItems().stream().filter(item -> !item.getAttachmentEntities().isEmpty()).findAny()
											.orElseThrow();
	}

	protected String putContentForAttachmentWithNavigation(Roots selectedRoot,
			Attachments itemAttachment) throws Exception {
		return putContentForAttachmentWithNavigation(selectedRoot, itemAttachment, status().isNoContent());
	}

	private String putContentForAttachmentWithNavigation(Roots selectedRoot, Attachments itemAttachment,
			ResultMatcher matcher) throws Exception {
		var selectedItem = selectedRoot.getItems().stream().filter(
						item -> item.getAttachments().stream().anyMatch(attach -> attach.getId().equals(itemAttachment.getId()))).findAny()
																							.orElseThrow();
		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), selectedItem.getId(),
				itemAttachment.getId()) + "/content";

		var testContent = "testContent" + itemAttachment.getNote();
		requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		requestHelper.executePutWithMatcher(url, testContent.getBytes(StandardCharsets.UTF_8), matcher);
		return testContent;
	}

	protected String buildNavigationAttachmentUrl(String rootId, String itemId, String attachmentId) {
		return "/odata/v4/TestService/Roots(" + rootId + ")/items(" + itemId + ")" + "/attachments(ID=" + attachmentId + ",up__ID=" + itemId + ")";
	}

	protected String buildExpandAttachmentUrl(String rootId, String itemId) {
		return "/odata/v4/TestService/Roots(" + rootId + ")/items(" + itemId + ")" + "?$expand=attachments,attachmentEntities";
	}

	private String putContentForAttachmentWithoutNavigation(AttachmentEntity itemAttachment) throws Exception {
		return putContentForAttachmentWithoutNavigation(itemAttachment, status().isNoContent());
	}

	private String putContentForAttachmentWithoutNavigation(AttachmentEntity itemAttachment,
			ResultMatcher matcher) throws Exception {
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

	private void executeDeleteAndCheckNoDataCanBeRead(String baseUrl, String contentId) throws Exception {
		var url = baseUrl + "/content";
		requestHelper.executeDelete(url);
		verifySingleDeletionEvent(contentId);
		clearServiceHandlerContext();
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentLength()).isZero();
		assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	protected abstract void executeContentRequestAndValidateContent(String url, String content) throws Exception;

	protected abstract void verifyTwoDeleteEvents(AttachmentEntity itemAttachmentEntityAfterChange,
			Attachments itemAttachmentAfterChange);

	protected abstract void verifyNumberOfEvents(String event, int number);

	protected abstract void verifyContentId(Attachments attachmentWithExpectedContent, String attachmentId,
			String contentId);

	protected abstract void verifyContentAndContentId(Attachments attachment, String testContent,
			Attachments itemAttachment) throws IOException;

	protected abstract void verifyContentAndContentIdForAttachmentEntity(AttachmentEntity attachment, String testContent,
			AttachmentEntity itemAttachment) throws IOException;

	protected abstract void clearServiceHandlerContext();

	protected abstract void clearServiceHandlerDocuments();

	protected abstract void verifySingleCreateEvent(String contentId, String content);

	protected abstract void verifySingleCreateAndUpdateEvent(String resultContentId, String toBeDeletedContentId,
			String content);

	protected abstract void verifySingleDeletionEvent(String contentId);

	protected abstract void verifySingleReadEvent(String contentId);

	protected abstract void verifyNoAttachmentEventsCalled();

	protected abstract void verifyEventContextEmptyForEvent(String... events);

}

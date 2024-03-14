package com.sap.cds.feature.attachments.integrationtests.nondraftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.AttachmentEntity_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.common.MockHttpRequestHelper;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.AttachmentsEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.ItemEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.RootEntityBuilder;
import com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper.TableDataDeleter;
import com.sap.cds.feature.attachments.integrationtests.testhandler.TestPluginAttachmentsServiceHandler;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class OdataRequestValidationWithoutTestHandlerTest {

	@Autowired
	private MockHttpRequestHelper requestHelper;
	@Autowired
	private PersistenceService persistenceService;
	@Autowired
	private TableDataDeleter dataDeleter;
	@Autowired
	private Optional<TestPluginAttachmentsServiceHandler> serviceHandler;

	@AfterEach
	void teardown() {
		dataDeleter.deleteData(Roots_.CDS_NAME);
	}

	@BeforeEach
	void setup() {
		requestHelper.resetHelper();
	}

	@Test
	void serviceHandlerIsEmpty() {
		assertThat(serviceHandler).isEmpty();
	}

	@Test
	void deepCreateWorks() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		verifySelectedRoot(selectedRoot, serviceRoot);
	}

	@Test
	void putContentWorksForUrlsWithNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		var testContent = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);
		var attachment = selectUpdatedAttachmentWithExpand(selectedRoot, itemAttachment);

		verifyContentAndDocumentId(attachment, testContent, itemAttachment);
	}

	@Test
	void putContentWorksForUrlsWithoutNavigation() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);

		var testContent = putContentForAttachmentWithoutNavigation(itemAttachment);
		var attachment = selectUpdatedAttachment(itemAttachment);

		assertThat(attachment.getContent().readAllBytes()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getId());
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
	}

	@Test
	void navigationReadOfAttachmentsHasFilledContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);

		var url = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSameSizeAs(item.getAttachments());

		var attachmentWithExpectedContent = responseItem.getAttachments().stream()
																																								.filter(attach -> attach.getId().equals(itemAttachment.getId())).findAny()
																																								.orElseThrow();
		assertThat(attachmentWithExpectedContent).containsEntry("content@mediaContentType", "application/octet-stream;charset=UTF-8")
				.containsEntry(Attachments.DOCUMENT_ID, itemAttachment.getId())
				.containsEntry(Attachments.FILE_NAME, itemAttachment.getFileName());
	}

	@Test
	void navigationReadOfAttachmentsReturnsContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		var content = putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);

		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId()) + "/content";
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
	}

	@Test
	void navigationDeleteOfContentClears() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);

		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId()) + "/content";
		requestHelper.executeDelete(url);
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentLength()).isZero();
		assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());

		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachments()).hasSameSizeAs(item.getAttachments());
		assertThat(responseItem.getAttachments()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getDocumentId()).isNull();
		});
	}

	@Test
	void navigationDeleteOfAttachmentClearsContentField() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);

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
	}

	@Test
	void navigationDeleteCallsTwiceReturnsError() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachment(selectedRoot);
		var itemAttachment = getRandomItemAttachment(item);
		putContentForAttachmentWithNavigation(selectedRoot, itemAttachment);

		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), item.getId(), itemAttachment.getId());
		requestHelper.executeDelete(url);
		var result = requestHelper.executeDelete(url);

		assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
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
	}

	@Test
	void directReadOfAttachmentsHasFilledContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		var responseAttachment = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(url, Attachments.class, HttpStatus.OK);

		assertThat(responseAttachment).containsEntry("content@mediaContentType", "application/octet-stream;charset=UTF-8")
				.containsEntry(Attachments.DOCUMENT_ID, itemAttachment.getId())
				.containsEntry(Attachments.FILE_NAME, itemAttachment.getFileName());
	}

	@Test
	void directReadOfAttachmentsReturnsContent() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		var content = putContentForAttachmentWithoutNavigation(itemAttachment);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId()) + "/content";
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentAsString()).isEqualTo(content);
	}

	@Test
	void directDeleteOfContentClears() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId()) + "/content";
		requestHelper.executeDelete(url);
		var response = requestHelper.executeGet(url);

		assertThat(response.getResponse().getContentLength()).isZero();
		assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.NO_CONTENT.value());

		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachmentEntities()).hasSameSizeAs(item.getAttachmentEntities());
		assertThat(responseItem.getAttachmentEntities()).allSatisfy(attachment -> {
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.get("content@mediaContentType")).isNull();
			assertThat(attachment.getDocumentId()).isNull();
		});
	}

	@Test
	void directDeleteOfAttachmentClearsContentField() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		requestHelper.executeDelete(url);
		var expandUrl = buildExpandAttachmentUrl(selectedRoot.getId(), item.getId());
		var responseItem = requestHelper.executeGetWithSingleODataResponseAndAssertStatus(expandUrl, Items.class, HttpStatus.OK);

		assertThat(responseItem.getAttachmentEntities()).isEmpty();
	}

	@Test
	void directDeleteCalledTwiceReturnsError() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();
		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		var item = getItemWithAttachmentEntity(selectedRoot);
		var itemAttachment = getRandomItemAttachmentEntity(item);
		putContentForAttachmentWithoutNavigation(itemAttachment);

		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId());
		requestHelper.executeDelete(url);
		MvcResult mvcResult = requestHelper.executeDelete(url);

		assertThat(mvcResult.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
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
		assertThat(selectedRoot.getItems().get(1).getAttachments()).isEmpty();
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
		var selectedItem = selectedRoot.getItems().stream().filter(item -> item.getAttachments().contains(itemAttachment))
																							.findAny().orElseThrow();
		var url = buildNavigationAttachmentUrl(selectedRoot.getId(), selectedItem.getId(), itemAttachment.getId()) + "/content";

		var testContent = "testContent";
		requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		requestHelper.executePutWithMatcher(url, testContent.getBytes(StandardCharsets.UTF_8), status().isNoContent());
		return testContent;
	}

	private String buildNavigationAttachmentUrl(String rootId, String itemId, String attachmentId) {
		return "/odata/v4/TestService/Roots(" + rootId + ")/items(" + itemId + ")" + "/attachments(ID=" + attachmentId + ",up__ID=" + itemId + ")";
	}

	private String buildExpandAttachmentUrl(String rootId, String itemId) {
		return "/odata/v4/TestService/Roots(" + rootId + ")/items(" + itemId + ")" + "?$expand=attachments,attachmentEntities";
	}

	private String putContentForAttachmentWithoutNavigation(AttachmentEntity itemAttachment) throws Exception {
		var url = buildDirectAttachmentEntityUrl(itemAttachment.getId()) + "/content";
		var testContent = "testContent";
		requestHelper.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		requestHelper.executePutWithMatcher(url, testContent.getBytes(StandardCharsets.UTF_8), status().isNoContent());
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

	private void verifyContentAndDocumentId(Attachments attachment, String testContent, Attachments itemAttachment) throws IOException {
		assertThat(attachment.getContent().readAllBytes()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getId());
	}

}

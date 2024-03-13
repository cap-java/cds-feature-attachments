package com.sap.cds.feature.attachments.integrationtests.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Items_;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots;
import com.sap.cds.feature.attachments.generated.integration.test.cds4j.testservice.Roots_;
import com.sap.cds.feature.attachments.integrationtests.constants.Profiles;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.StructuredType;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(Profiles.TEST_HANDLER_DISABLED)
class OdataRequestValidationWithoutTestHandlerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private PersistenceService persistenceService;

	@Test
	void deepCreateAndChangeStory() throws Exception {
		var serviceRoot = buildServiceRootWithDeepData();

		postServiceRoot(serviceRoot);

		var selectedRoot = selectStoredRootWithDeepData();
		verifySelectedRoot(selectedRoot, serviceRoot);

		var itemAttachment = getRandomItemAttachment(selectedRoot);
		var testContent = putContentForAttachment(selectedRoot, itemAttachment);
		var attachment = selectUpdatedAttachment(selectedRoot, itemAttachment);

		verifyContentAndDocumentId(attachment, testContent, itemAttachment);
	}

	private Roots buildServiceRootWithDeepData() {
		var serviceRoot = Roots.create();
		serviceRoot.setTitle("some root title");
		var attachmentRoot = buildAttachment("fileRoot.txt");
		serviceRoot.setAttachments(List.of(attachmentRoot));
		var item1 = Items.create();
		item1.setTitle("some item 1 title");
		var attachmentItem1 = buildAttachment("fileItem1.txt");
		var attachmentItem2 = buildAttachment("fileItem2.txt");
		item1.setAttachments(List.of(attachmentItem1, attachmentItem2));
		var item2 = Items.create();
		item2.setTitle("some item 2 title");
		serviceRoot.setItems(List.of(item1, item2));
		return serviceRoot;
	}

	private Attachments buildAttachment(String file) {
		var attachmentRoot = Attachments.create();
		attachmentRoot.setFileName(file);
		attachmentRoot.setMimeType("text/plain");
		return attachmentRoot;
	}

	private void postServiceRoot(Roots serviceRoot) throws Exception {
		mockMvc.perform(post("/odata/v4/TestService/Roots").contentType(MediaType.APPLICATION_JSON).content(serviceRoot.toJson()))
				.andExpect(status().isCreated());
	}

	private Roots selectStoredRootWithDeepData() {
		CqnSelect select = Select.from(Roots_.class).columns(StructuredType::_all, root -> root.attachments().expand(), root -> root.items().expand(StructuredType::_all, item -> item.attachments().expand()));
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

	private Attachments getRandomItemAttachment(Roots selectedRoot) {
		return selectedRoot.getItems().get(0).getAttachments().get(0);
	}

	private String putContentForAttachment(Roots selectedRoot, Attachments itemAttachment) throws Exception {
		var url = "/odata/v4/TestService/Roots(" + selectedRoot.getId() + ")/items(" + selectedRoot.getItems().get(0).getId() + ")/attachments(ID=" + itemAttachment.getId() + ",up__ID=" + selectedRoot.getItems().get(0).getId() + ")/content";
		var testContent = "testContent";
		mockMvc.perform(put(url).contentType(MediaType.APPLICATION_OCTET_STREAM).content(testContent.getBytes(StandardCharsets.UTF_8)))
				.andExpect(status().isNoContent());
		return testContent;
	}

	private Attachments selectUpdatedAttachment(Roots selectedRoot, Attachments itemAttachment) {
		CqnSelect attachmentSelect = Select.from(Items_.class).where(a -> a.ID().eq(selectedRoot.getItems().get(0).getId())).columns(item -> item.attachments().expand());
		var result = persistenceService.run(attachmentSelect);
		var items = result.single(Items.class);
		return items.getAttachments().stream().filter(attach -> itemAttachment.getId().equals(attach.getId())).findAny().orElseThrow();
	}

	private void verifyContentAndDocumentId(Attachments attachment, String testContent, Attachments itemAttachment) throws IOException {
		assertThat(attachment.getContent().readAllBytes()).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
		assertThat(attachment.getDocumentId()).isEqualTo(itemAttachment.getId());
	}

}

package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sap.cds.Result;
import com.sap.cds.feature.attachments.configuration.Registration;
import com.sap.cds.feature.attachments.generation.test.cds4j.com.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Attachment;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.CreateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.ReadAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.applicationservice.UpdateAttachmentsHandler;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.LazyProxyInputStream;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.UpdateAttachmentInput;
import com.sap.cds.impl.RowImpl;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Selectable;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;

class AttachmentsHandlerIntegratedTest extends Registration {

	private static CdsRuntime runtime;
	private CreateAttachmentsHandler createHandler;
	private UpdateAttachmentsHandler updateHandler;
	private ReadAttachmentsHandler readHandler;
	private PersistenceService persistenceService;
	private AttachmentService attachmentService;
	private CdsCreateEventContext createContext;
	private CdsUpdateEventContext updateContext;
	private CdsReadEventContext readContext;
	private ArgumentCaptor<CreateAttachmentInput> createEventInputCaptor;
	private ArgumentCaptor<UpdateAttachmentInput> updateEventInputCaptor;
	private ArgumentCaptor<CqnSelect> selectArgumentCaptor;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		persistenceService = mock(PersistenceService.class);
		attachmentService = mock(AttachmentService.class);

		createHandler = (CreateAttachmentsHandler) buildCreateHandler(persistenceService, buildAttachmentEventFactory(attachmentService));
		updateHandler = (UpdateAttachmentsHandler) buildUpdateHandler(persistenceService, buildAttachmentEventFactory(attachmentService));
		readHandler = (ReadAttachmentsHandler) buildReadHandler(attachmentService);

		createContext = mock(CdsCreateEventContext.class);
		updateContext = mock(CdsUpdateEventContext.class);
		readContext = mock(CdsReadEventContext.class);
		createEventInputCaptor = ArgumentCaptor.forClass(CreateAttachmentInput.class);
		updateEventInputCaptor = ArgumentCaptor.forClass(UpdateAttachmentInput.class);
		selectArgumentCaptor = ArgumentCaptor.forClass(CqnSelect.class);
	}

	@Nested
	@DisplayName("Tests for calling the CREATE event")
	class CreateContentTests {

		@Test
		void simpleCreateDoesNotCallAttachment() {
			var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
			when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);
			var roots = RootTable.create();

			createHandler.processBefore(createContext, List.of(roots));

			verifyNoInteractions(attachmentService);
			assertThat(roots.getId()).isNull();
		}

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void simpleCreateCallsAttachment(boolean isExternalStored) throws IOException {
			var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);
			when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(isExternalStored, "document id"));

			var attachment = Attachments.create();

			var testString = "test";
			var fileName = "testFile.txt";
			var mimeType = "test/type";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
				attachment.setContent(testStream);
				attachment.setFileName(fileName);
				attachment.setMimeType(mimeType);

				createHandler.processBefore(createContext, List.of(attachment));

				verify(attachmentService).createAttachment(createEventInputCaptor.capture());
				var createInput = createEventInputCaptor.getValue();
				var expectedContent = isExternalStored ? null : testStream;
				assertThat(attachment.getContent()).isEqualTo(expectedContent);
				assertThat(createInput.content()).isEqualTo(testStream);
				assertThat(createInput.attachmentIds()).isNotEmpty().containsEntry("ID", attachment.getId());
				assertThat(createInput.attachmentEntityName()).isNotEmpty().isEqualTo(Attachment_.CDS_NAME);
				assertThat(createInput.fileName()).isEqualTo(fileName);
				assertThat(createInput.mimeType()).isEqualTo(mimeType);
			}
		}

		@Test
		void deepCreateDoesNoCallAttachmentService() {
			var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
			when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);

			var fileName = "testFile.txt";
			var mimeType = "test/type";

			var roots = RootTable.create();
			var item = Items.create();
			var attachment = Attachments.create();
			attachment.setFileName(fileName);
			attachment.setMimeType(mimeType);
			item.setAttachments(List.of(attachment));
			roots.setItems(List.of(item));

			createHandler.processBefore(createContext, List.of(roots));

			verifyNoInteractions(attachmentService);
		}

		@Test
		void deepCreateCallsAttachmentService() throws IOException {
			var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
			when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);
			when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "document id"));

			var testString = "test";
			var fileName = "testFile.txt";
			var mimeType = "test/type";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
				var roots = RootTable.create();
				var item = Items.create();
				var attachment = Attachments.create();
				attachment.setContent(testStream);
				attachment.setFileName(fileName);
				attachment.setMimeType(mimeType);
				item.setAttachments(List.of(attachment));
				roots.setItems(List.of(item));

				createHandler.processBefore(createContext, List.of(roots));

				verify(attachmentService).createAttachment(createEventInputCaptor.capture());
				var input = createEventInputCaptor.getValue();
				assertThat(attachment.getContent()).isEqualTo(testStream);
				assertThat(input.content()).isEqualTo(testStream);
				assertThat(input.attachmentIds()).isNotEmpty().containsEntry("ID", attachment.getId());
				assertThat(input.attachmentEntityName()).isNotEmpty().isEqualTo(Attachment_.CDS_NAME);
				assertThat(input.fileName()).isEqualTo(fileName);
				assertThat(input.mimeType()).isEqualTo(mimeType);
			}
		}

	}

	@Nested
	@DisplayName("Tests for calling the UPDATE event")
	class UpdateContentTests {

		@Test
		void deepUpdateCallsAttachment() throws IOException {
			var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
			when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);
			when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "document id"));
			var existingData = mockExistingData();

			var testString = "test";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {

				var roots = RootTable.create();
				roots.setTitle("new title");
				roots.setId(UUID.randomUUID().toString());
				var attachmentAspect = Attachment.create();
				attachmentAspect.setContent(testStream);
				attachmentAspect.setId(UUID.randomUUID().toString());
				roots.setAttachmentTable(List.of(attachmentAspect));

				updateHandler.processBefore(updateContext, List.of(roots));

				verify(attachmentService).createAttachment(createEventInputCaptor.capture());
				var creationInput = createEventInputCaptor.getValue();
				assertThat(attachmentAspect.getContent()).isEqualTo(testStream);
				assertThat(creationInput.content()).isEqualTo(testStream);
				assertThat(creationInput.attachmentIds()).isNotEmpty().containsEntry("ID", attachmentAspect.getId());
				assertThat(creationInput.attachmentEntityName()).isNotEmpty().isEqualTo(RootTable_.CDS_NAME + ".attachments");
				assertThat(creationInput.fileName()).isEqualTo(existingData.getFileName());
				assertThat(creationInput.mimeType()).isEqualTo(existingData.getMimeType());
				verify(persistenceService).run(selectArgumentCaptor.capture());
				var select = selectArgumentCaptor.getValue();
				assertThat(select.where().orElseThrow().toString()).contains(attachmentAspect.getId());

			}
		}

		@Test
		void simpleUpdateDoesNotCallAttachment() {
			var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
			when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);

			var roots = RootTable.create();
			roots.setTitle("new title");

			updateHandler.processBefore(updateContext, List.of(roots));

			verifyNoInteractions(attachmentService);
			assertThat(roots.getId()).isNull();
		}

		@Test
		void simpleUpdateCallsAttachmentWithCreate() throws IOException {
			var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);
			when(attachmentService.createAttachment(any())).thenReturn(new AttachmentModificationResult(false, "document id"));
			var existingData = mockExistingData();

			var attachment = Attachments.create();
			var testString = "test";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
				attachment.setContent(testStream);
				attachment.setId(UUID.randomUUID().toString());

				updateHandler.processBefore(updateContext, List.of(attachment));

				verify(attachmentService).createAttachment(createEventInputCaptor.capture());
				var creationInput = createEventInputCaptor.getValue();
				assertThat(attachment.getContent()).isEqualTo(testStream);
				assertThat(creationInput.content()).isEqualTo(testStream);
				assertThat(creationInput.attachmentIds()).isNotEmpty().containsEntry("ID", attachment.getId());
				assertThat(creationInput.attachmentEntityName()).isNotEmpty().isEqualTo(Attachment_.CDS_NAME);
				assertThat(creationInput.fileName()).isEqualTo(existingData.getFileName());
				assertThat(creationInput.mimeType()).isEqualTo(existingData.getMimeType());
				verify(persistenceService).run(selectArgumentCaptor.capture());
				var select = selectArgumentCaptor.getValue();
				assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
			}
		}

		@Test
		void simpleUpdateCallsAttachmentWithUpdate() throws IOException {
			var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);
			when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentModificationResult(false, "document id"));
			var existingData = mockExistingData();
			existingData.setDocumentId(UUID.randomUUID().toString());

			var attachment = Attachments.create();
			var testString = "test";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
				attachment.setContent(testStream);
				attachment.setId(UUID.randomUUID().toString());

				updateHandler.processBefore(updateContext, List.of(attachment));

				verify(attachmentService).updateAttachment(updateEventInputCaptor.capture());
				var updateInput = updateEventInputCaptor.getValue();
				assertThat(attachment.getContent()).isEqualTo(testStream);
				assertThat(updateInput.content()).isEqualTo(testStream);
				assertThat(updateInput.attachmentIds()).isNotEmpty().containsEntry("ID", attachment.getId());
				assertThat(updateInput.attachmentEntityName()).isNotEmpty().isEqualTo(Attachment_.CDS_NAME);
				assertThat(updateInput.fileName()).isEqualTo(existingData.getFileName());
				assertThat(updateInput.mimeType()).isEqualTo(existingData.getMimeType());
				verify(persistenceService).run(selectArgumentCaptor.capture());
				var select = selectArgumentCaptor.getValue();
				assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
			}
		}

		@Test
		void removeValueFromContentDeletesDocument() {
			var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
			when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
			when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);
			var result = mock(Result.class);
			var oldData = Attachments.create();
			oldData.setDocumentId(UUID.randomUUID().toString());
			oldData.setId(UUID.randomUUID().toString());
			var row = RowImpl.row(oldData);
			when(result.single()).thenReturn(row);
			when(result.rowCount()).thenReturn(1L);
			when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);

			var attachment = Attachments.create();
			attachment.setId(oldData.getId());
			attachment.setContent(null);

			updateHandler.processBefore(updateContext, List.of(attachment));

			verify(attachmentService).deleteAttachment(oldData.getDocumentId());
			assertThat(attachment.getContent()).isNull();
			assertThat(attachment.getDocumentId()).isNull();
			verifyNoMoreInteractions(attachmentService);
			verify(persistenceService).run(selectArgumentCaptor.capture());
			var select = selectArgumentCaptor.getValue();
			assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
		}

		private Attachments mockExistingData() {
			var result = mock(Result.class);
			var oldData = Attachments.create();
			oldData.setFileName("some file name");
			oldData.setMimeType("some mime type");
			var row = RowImpl.row(oldData);
			when(result.rowCount()).thenReturn(1L);
			when(result.single()).thenReturn(row);
			when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);
			return oldData;
		}

	}

	@Nested
	@DisplayName("Tests for calling the READ event")
	class ReadContentTests {

		@Test
		void expandedReadAddsDocumentId() {
			CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, getItemExpandWithContent()));
			mockReadContext(select, RootTable_.CDS_NAME);

			readHandler.processBefore(readContext);

			verify(readContext).setCqn(selectArgumentCaptor.capture());
			var resultCqn = selectArgumentCaptor.getValue();
			assertThat(resultCqn.toString()).contains("documentId");
		}

		@Test
		void expandedReadWithoutContentDoNotAddDocumentId() {
			CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, getItemExpandWithAllFields()));
			mockReadContext(select, RootTable_.CDS_NAME);

			readHandler.processBefore(readContext);

			verify(readContext).setCqn(selectArgumentCaptor.capture());
			var resultCqn = selectArgumentCaptor.getValue();
			assertThat(resultCqn.toString()).doesNotContain("documentId");
		}

		@Test
		void directReadForAttachmentsAddsDocumentId() {
			CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::ID, Attachment_::content);
			mockReadContext(select, Attachment_.CDS_NAME);

			readHandler.processBefore(readContext);

			verify(readContext).setCqn(selectArgumentCaptor.capture());
			var resultCqn = selectArgumentCaptor.getValue();
			assertThat(resultCqn.toString()).contains("documentId");
		}

		@Test
		void directReadForAttachmentsWithNoFieldsDoesNotInsertDocumentId() {
			CqnSelect select = Select.from(Attachment_.class);
			mockReadContext(select, Attachment_.CDS_NAME);

			readHandler.processBefore(readContext);

			verify(readContext).setCqn(selectArgumentCaptor.capture());
			var resultCqn = selectArgumentCaptor.getValue();
			assertThat(resultCqn.toString()).doesNotContain("documentId");
		}

		@Test
		void directReadForAttachmentsWithDocumentIdDoesNotInsertDocumentId() {
			CqnSelect select = Select.from(Attachment_.class).columns(Attachment_::documentId, Attachment_::ID, Attachment_::content);
			mockReadContext(select, Attachment_.CDS_NAME);

			readHandler.processBefore(readContext);

			verify(readContext).setCqn(selectArgumentCaptor.capture());
			var resultCqn = selectArgumentCaptor.getValue();
			assertThat(resultCqn.toString()).containsOnlyOnce("documentId");
		}

		@Test
		void streamProxyIsInsertedButNotCalledForSingleRequest() {
			mockReadContext(mock(CqnSelect.class), Attachment_.CDS_NAME);

			var attachment = Attachments.create();
			attachment.setDocumentId("some ID");
			attachment.setContent(null);

			readHandler.processAfter(readContext, List.of(attachment));

			assertThat(attachment.getContent()).isInstanceOf(LazyProxyInputStream.class);
			verifyNoInteractions(attachmentService);
		}

		@Test
		void streamProxyIsInsertedButNotCalledForDeepRequest() throws IOException {
			mockReadContext(mock(CqnSelect.class), RootTable_.CDS_NAME);

			var testString = "test";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {

				var attachmentWithNullContent = Attachments.create();
				attachmentWithNullContent.setDocumentId("some ID");
				attachmentWithNullContent.setContent(null);
				var item1 = Items.create();
				item1.setId("item id1");
				item1.setAttachments(List.of(attachmentWithNullContent));
				var attachmentWithoutContent = Attachments.create();
				attachmentWithoutContent.setDocumentId("some ID");
				var item2 = Items.create();
				item2.setId("item id2");
				item2.setAttachments(List.of(attachmentWithoutContent));
				var item3 = Items.create();
				item3.setId("item id3");
				var attachmentWithFilledContent = Attachments.create();
				attachmentWithFilledContent.setDocumentId("some ID");
				attachmentWithFilledContent.setContent(testStream);
				var item4 = Items.create();
				item4.setId("item id4");
				item4.setAttachments(List.of(attachmentWithFilledContent));
				var attachmentWithFilledContentButWithoutDocumentId = Attachments.create();
				attachmentWithFilledContentButWithoutDocumentId.setContent(null);
				var item5 = Items.create();
				item5.setId("item id4");
				item5.setAttachments(List.of(attachmentWithFilledContentButWithoutDocumentId));
				var root1 = RootTable.create();
				root1.setItems(List.of(item2, item1, item4, item5));
				var root2 = RootTable.create();
				root2.setItems(List.of(item3));

				readHandler.processAfter(readContext, List.of(root2, root1));

				assertThat(attachmentWithNullContent.getContent()).isInstanceOf(LazyProxyInputStream.class);
				assertThat(attachmentWithoutContent.getContent()).isNull();
				assertThat(attachmentWithFilledContent.getContent()).isEqualTo(testStream);
				assertThat(attachmentWithFilledContentButWithoutDocumentId.getContent()).isNull();
				verifyNoInteractions(attachmentService);
			}
		}

		@Test
		void attachmentServiceCalledIfStreamIsRequested() throws IOException {
			mockReadContext(mock(CqnSelect.class), Attachment_.CDS_NAME);

			var testString = "test";
			try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
				when(attachmentService.readAttachment(any())).thenReturn(testStream);
				var attachment = Attachments.create();
				attachment.setDocumentId("some ID");
				attachment.setContent(null);

				readHandler.processAfter(readContext, List.of(attachment));

				assertThat(attachment.getContent()).isInstanceOf(LazyProxyInputStream.class);
				verifyNoInteractions(attachmentService);
				byte[] bytes = attachment.getContent().readAllBytes();
				assertThat(bytes).isEqualTo(testString.getBytes(StandardCharsets.UTF_8));
				verify(attachmentService).readAttachment(attachment.getDocumentId());
			}

		}

		private void mockReadContext(CqnSelect select, String entityName) {
			var serviceEntity = runtime.getCdsModel().findEntity(entityName);
			when(readContext.getCqn()).thenReturn(select);
			when(readContext.getCdsRuntime()).thenReturn(runtime);
			when(readContext.getModel()).thenReturn(runtime.getCdsModel());
			when(readContext.getEvent()).thenReturn(CqnService.EVENT_READ);
			when(readContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
		}

		private Function<Items_, Selectable> getItemExpandWithContent() {
			return item -> item.attachments().expand(Attachment_::content);
		}

		private Function<Items_, Selectable> getItemExpandWithAllFields() {
			return item -> item.attachments().expand();
		}

	}

}

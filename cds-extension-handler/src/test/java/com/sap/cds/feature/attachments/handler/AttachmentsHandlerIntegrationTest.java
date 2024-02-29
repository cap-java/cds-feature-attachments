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
import com.sap.cds.feature.attachments.handler.configuration.AutoConfiguration;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
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

class AttachmentsHandlerIntegrationTest {

		private static CdsRuntime runtime;
		private AttachmentsHandler cut;
		private PersistenceService persistenceService;
		private AttachmentService attachmentService;
		private CdsCreateEventContext createContext;
		private CdsUpdateEventContext updateContext;
		private CdsReadEventContext readContext;
		private ArgumentCaptor<AttachmentCreateEventContext> createEventInputCaptor;
		private ArgumentCaptor<AttachmentUpdateEventContext> updateEventInputCaptor;
		private ArgumentCaptor<AttachmentDeleteEventContext> deleteEventInputCaptor;
		private ArgumentCaptor<CqnSelect> selectArgumentCaptor;

		@BeforeAll
		static void classSetup() {
				runtime = new RuntimeHelper().runtime;
		}

		@BeforeEach
		void setup() {
				persistenceService = mock(PersistenceService.class);
				attachmentService = mock(AttachmentService.class);

				cut = (AttachmentsHandler) new AutoConfiguration().buildHandler(persistenceService, attachmentService);

				createContext = mock(CdsCreateEventContext.class);
				updateContext = mock(CdsUpdateEventContext.class);
				readContext = mock(CdsReadEventContext.class);
				createEventInputCaptor = ArgumentCaptor.forClass(AttachmentCreateEventContext.class);
				updateEventInputCaptor = ArgumentCaptor.forClass(AttachmentUpdateEventContext.class);
				deleteEventInputCaptor = ArgumentCaptor.forClass(AttachmentDeleteEventContext.class);
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

						cut.uploadAttachments(createContext, List.of(roots));

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

						var attachment = Attachment.create();

						var testString = "test";
						var fileName = "testFile.txt";
						var mimeType = "test/type";
						try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
								attachment.setContent(testStream);
								attachment.setFilename(fileName);
								attachment.setMimeType(mimeType);
								attachment.setParentKey(UUID.randomUUID().toString());

								cut.uploadAttachments(createContext, List.of(attachment));

								verify(attachmentService).createAttachment(createEventInputCaptor.capture());
								var createInput = createEventInputCaptor.getValue();
								var expectedContent = isExternalStored ? null : testStream;
								assertThat(attachment.getContent()).isEqualTo(expectedContent);
								assertThat(createInput.getContent()).isEqualTo(testStream);
								assertThat(createInput.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(createInput.getFileName()).isEqualTo(fileName);
								assertThat(createInput.getMimeType()).isEqualTo(mimeType);
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
						var attachment = Attachment.create();
						attachment.setFilename(fileName);
						attachment.setMimeType(mimeType);
						item.setAttachments(List.of(attachment));
						roots.setItems(List.of(item));

						cut.uploadAttachments(createContext, List.of(roots));

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
								var attachment = Attachment.create();
								attachment.setContent(testStream);
								attachment.setFilename(fileName);
								attachment.setMimeType(mimeType);
								item.setAttachments(List.of(attachment));
								roots.setItems(List.of(item));

								cut.uploadAttachments(createContext, List.of(roots));

								verify(attachmentService).createAttachment(createEventInputCaptor.capture());
								var input = createEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isEqualTo(testStream);
								assertThat(input.getContent()).isEqualTo(testStream);
								assertThat(input.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(input.getFileName()).isEqualTo(fileName);
								assertThat(input.getMimeType()).isEqualTo(mimeType);
						}
				}

		}

		@Nested
		@DisplayName("Tests for calling the UPDATE event")
		class UpdateContentTests {

				@Test
				void simpleUpdateDoesNotCallAttachment() {
						var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
						when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
						when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);

						var roots = RootTable.create();
						roots.setTitle("new title");

						cut.uploadAttachments(updateContext, List.of(roots));

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

						var attachment = Attachment.create();
						var testString = "test";
						try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
								attachment.setContent(testStream);
								attachment.setId(UUID.randomUUID().toString());

								cut.uploadAttachments(updateContext, List.of(attachment));

								verify(attachmentService).createAttachment(createEventInputCaptor.capture());
								var creationInput = createEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isEqualTo(testStream);
								assertThat(creationInput.getContent()).isEqualTo(testStream);
								assertThat(creationInput.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(creationInput.getFileName()).isEqualTo(existingData.getFilename());
								assertThat(creationInput.getMimeType()).isEqualTo(existingData.getMimeType());
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

						var attachment = Attachment.create();
						var testString = "test";
						try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
								attachment.setContent(testStream);
								attachment.setId(UUID.randomUUID().toString());

								cut.uploadAttachments(updateContext, List.of(attachment));

								verify(attachmentService).updateAttachment(updateEventInputCaptor.capture());
								var updateInput = updateEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isEqualTo(testStream);
								assertThat(updateInput.getContent()).isEqualTo(testStream);
								assertThat(updateInput.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(updateInput.getFileName()).isEqualTo(existingData.getFilename());
								assertThat(updateInput.getMimeType()).isEqualTo(existingData.getMimeType());
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
						var oldData = Attachment.create();
						oldData.setDocumentId(UUID.randomUUID().toString());
						oldData.setId(UUID.randomUUID().toString());
						var row = RowImpl.row(oldData);
						when(result.single()).thenReturn(row);
						when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);

						var attachment = Attachment.create();
						attachment.setId(oldData.getId());
						attachment.setContent(null);

						cut.uploadAttachments(updateContext, List.of(attachment));

						verify(attachmentService).deleteAttachment(deleteEventInputCaptor.capture());
						var deleteInput = deleteEventInputCaptor.getValue();
						assertThat(attachment.getContent()).isNull();
						assertThat(attachment.getDocumentId()).isNull();
						assertThat(deleteInput.getDocumentId()).isEqualTo(oldData.getDocumentId());
						verifyNoMoreInteractions(attachmentService);
						verify(persistenceService).run(selectArgumentCaptor.capture());
						var select = selectArgumentCaptor.getValue();
						assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
				}

				private Attachment mockExistingData() {
						var result = mock(Result.class);
						var oldData = Attachment.create();
						oldData.setFilename("some file name");
						oldData.setMimeType("some mime type");
						var row = RowImpl.row(oldData);
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
						var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
						when(readContext.getCqn()).thenReturn(select);
						when(readContext.getCdsRuntime()).thenReturn(runtime);
						when(readContext.getEvent()).thenReturn(CqnService.EVENT_READ);
						when(readContext.getTarget()).thenReturn(serviceEntity.orElseThrow());

						cut.readAttachmentsBeforeEvent(readContext);

						verify(readContext).setCqn(selectArgumentCaptor.capture());
						var resultCqn = selectArgumentCaptor.getValue();
						assertThat(resultCqn.toString()).contains("documentId");
				}

				@Test
				void expandedReadWithoutContentDoNotAddDocumentId() {
						CqnSelect select = Select.from(RootTable_.class).columns(RootTable_::ID, root -> root.items().expand(Items_::ID, getItemExpandWithAllFiels()));
						var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
						when(readContext.getCqn()).thenReturn(select);
						when(readContext.getCdsRuntime()).thenReturn(runtime);
						when(readContext.getEvent()).thenReturn(CqnService.EVENT_READ);
						when(readContext.getTarget()).thenReturn(serviceEntity.orElseThrow());

						cut.readAttachmentsBeforeEvent(readContext);

						verify(readContext).setCqn(selectArgumentCaptor.capture());
						var resultCqn = selectArgumentCaptor.getValue();
						assertThat(resultCqn.toString()).doesNotContain("documentId");
				}


				private Function<Items_, Selectable> getItemExpandWithContent() {
						return item -> item.attachments().expand(Attachment_::content);
				}

				private Function<Items_, Selectable> getItemExpandWithAllFiels() {
						return item -> item.attachments().expand();
				}

		}

}

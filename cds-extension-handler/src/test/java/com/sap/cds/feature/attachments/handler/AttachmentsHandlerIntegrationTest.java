package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.Result;
import com.sap.cds.feature.attachments.handler.configuration.AutoConfiguration;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentStorageResult;
import com.sap.cds.feature.attachments.service.model.AttachmentStoreEventContext;
import com.sap.cds.feature.attachments.service.model.AttachmentUpdateEventContext;
import com.sap.cds.impl.RowImpl;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.CdsCreateEventContext;
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
		private ArgumentCaptor<AttachmentStoreEventContext> storeEventInputCaptor;
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
				storeEventInputCaptor = ArgumentCaptor.forClass(AttachmentStoreEventContext.class);
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

				@Test
				void simpleCreateCallsAttachment() throws AttachmentAccessException, IOException {
						var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
						when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
						when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);
						when(attachmentService.storeAttachment(any())).thenReturn(new AttachmentStorageResult(false, "document id"));

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

								verify(attachmentService).storeAttachment(storeEventInputCaptor.capture());
								var input = storeEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isEqualTo(testStream);
								assertThat(input.getContent()).isEqualTo(testStream);
								assertThat(input.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(input.getFileName()).isEqualTo(fileName);
								assertThat(input.getMimeType()).isEqualTo(mimeType);
						}
				}

				@Test
				void simpleCreateCallsAttachmentAndRemovesContent() throws AttachmentAccessException, IOException {
						var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
						when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
						when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);
						when(attachmentService.storeAttachment(any())).thenReturn(new AttachmentStorageResult(true, "document id"));

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

								verify(attachmentService).storeAttachment(storeEventInputCaptor.capture());
								var input = storeEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isNull();
								assertThat(input.getContent()).isEqualTo(testStream);
								assertThat(input.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(input.getFileName()).isEqualTo(fileName);
								assertThat(input.getMimeType()).isEqualTo(mimeType);
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
						roots.setItemTable(List.of(item));

						cut.uploadAttachments(createContext, List.of(roots));

						verifyNoInteractions(attachmentService);
				}

				@Test
				void deepCreateCallsAttachmentService() throws AttachmentAccessException, IOException {
						var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
						when(createContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
						when(createContext.getEvent()).thenReturn(CqnService.EVENT_CREATE);
						when(attachmentService.storeAttachment(any())).thenReturn(new AttachmentStorageResult(false, "document id"));

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
								roots.setItemTable(List.of(item));

								cut.uploadAttachments(createContext, List.of(roots));

								verify(attachmentService).storeAttachment(storeEventInputCaptor.capture());
								var input = storeEventInputCaptor.getValue();
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
				void simpleUpdateCallsAttachmentWithCreate() throws AttachmentAccessException, IOException {
						var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
						when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
						when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);
						when(attachmentService.storeAttachment(any())).thenReturn(new AttachmentStorageResult(false, "document id"));
						var result = mock(Result.class);
						var oldData = Attachment.create();
						oldData.setFilename("some file name");
						oldData.setMimeType("some mime type");
						var row = RowImpl.row(oldData);
						when(result.single()).thenReturn(row);
						when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);

						var attachment = Attachment.create();
						var testString = "test";
						try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
								attachment.setContent(testStream);
								attachment.setId(UUID.randomUUID().toString());

								cut.uploadAttachments(updateContext, List.of(attachment));

								verify(attachmentService).storeAttachment(storeEventInputCaptor.capture());
								var input = storeEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isEqualTo(testStream);
								assertThat(input.getContent()).isEqualTo(testStream);
								assertThat(input.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(input.getFileName()).isEqualTo(oldData.getFilename());
								assertThat(input.getMimeType()).isEqualTo(oldData.getMimeType());
								verify(persistenceService).run(selectArgumentCaptor.capture());
								var select = selectArgumentCaptor.getValue();
								assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
						}
				}

				@Test
				void simpleUpdateCallsAttachmentWithUpdate() throws AttachmentAccessException, IOException {
						var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
						when(updateContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
						when(updateContext.getEvent()).thenReturn(CqnService.EVENT_UPDATE);
						when(attachmentService.updateAttachment(any())).thenReturn(new AttachmentStorageResult(false, "document id"));
						var result = mock(Result.class);
						var oldData = Attachment.create();
						oldData.setFilename("some file name");
						oldData.setMimeType("some mime type");
						oldData.setDocumentId(UUID.randomUUID().toString());
						var row = RowImpl.row(oldData);
						when(result.single()).thenReturn(row);
						when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);

						var attachment = Attachment.create();
						var testString = "test";
						try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
								attachment.setContent(testStream);
								attachment.setId(UUID.randomUUID().toString());

								cut.uploadAttachments(updateContext, List.of(attachment));

								verify(attachmentService).updateAttachment(updateEventInputCaptor.capture());
								var input = updateEventInputCaptor.getValue();
								assertThat(attachment.getContent()).isEqualTo(testStream);
								assertThat(input.getContent()).isEqualTo(testStream);
								assertThat(input.getAttachmentId()).isNotEmpty().isEqualTo(attachment.getId());
								assertThat(input.getFileName()).isEqualTo(oldData.getFilename());
								assertThat(input.getMimeType()).isEqualTo(oldData.getMimeType());
								verify(persistenceService).run(selectArgumentCaptor.capture());
								var select = selectArgumentCaptor.getValue();
								assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
						}
				}

				@Test
				void removeValueFromContentDeletesDocument() throws AttachmentAccessException {
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
						var input = deleteEventInputCaptor.getValue();
						assertThat(attachment.getContent()).isNull();
						assertThat(attachment.getDocumentId()).isNull();
						assertThat(input.getDocumentId()).isEqualTo(oldData.getDocumentId());
						verifyNoMoreInteractions(attachmentService);
						verify(persistenceService).run(selectArgumentCaptor.capture());
						var select = selectArgumentCaptor.getValue();
						assertThat(select.where().orElseThrow().toString()).contains(attachment.getId());
				}

		}

}

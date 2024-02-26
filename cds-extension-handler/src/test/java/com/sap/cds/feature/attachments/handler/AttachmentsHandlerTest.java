package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.WrongAttachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.WrongAttachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.handler.model.AttachmentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.ApplicationEventProcessor;
import com.sap.cds.feature.attachments.handler.processor.AttachmentEvent;
import com.sap.cds.feature.attachments.service.AttachmentAccessException;
import com.sap.cds.impl.RowImpl;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;

class AttachmentsHandlerTest {

		private AttachmentsHandler cut;
		private PersistenceService persistenceService;
		private ApplicationEventProcessor eventProcessor;

		private CdsCreateEventContext createContext;
		private CdsUpdateEventContext updateContext;
		private CdsData cdsData;
		private ArgumentCaptor<AttachmentFieldNames> fieldNamesArgumentCaptor;
		private AttachmentEvent event;

		private static CdsRuntime runtime;

		@BeforeAll
		static void classSetup() {
				runtime = new RuntimeHelper().runtime;
		}

		@BeforeEach
		void setup() {
				persistenceService = mock(PersistenceService.class);

				eventProcessor = mock(ApplicationEventProcessor.class);
				cut = new AttachmentsHandler(persistenceService, eventProcessor);

				createContext = mock(CdsCreateEventContext.class);
				updateContext = mock(CdsUpdateEventContext.class);
				cdsData = mock(CdsData.class);
				fieldNamesArgumentCaptor = ArgumentCaptor.forClass(AttachmentFieldNames.class);
				event = mock(AttachmentEvent.class);
		}

		@Test
		void noProcessingNeededForCreate() {
				CdsEntity entity = mock(CdsEntity.class);
				when(createContext.getTarget()).thenReturn(entity);
				when(eventProcessor.isAttachmentEvent(entity, List.of(cdsData))).thenReturn(false);

				cut.uploadAttachmentsForCreate(createContext, List.of(cdsData));

				verifyNoInteractions(persistenceService);
				verify(eventProcessor).isAttachmentEvent(entity, List.of(cdsData));
		}

		@Test
		void idsAreSetInDataForCreate() {
				var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);
				var roots = RootTable.create();
				var attachment = Attachment.create();
				attachment.setFilename("test.txt");
				roots.setAttachments(List.of(attachment));
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(roots))).thenReturn(true);

				cut.uploadAttachmentsForCreate(createContext, List.of(roots));

				assertThat(roots.getId()).isNotEmpty();
				assertThat(attachment.getId()).isNotEmpty();
		}

		@Test
		void eventProcessorCalledForCreate() throws IOException {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);

				try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
						var attachment = Attachment.create();
						attachment.setContent(testStream);
						mockTargetInContext(serviceEntity.orElseThrow());
						when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).thenReturn(true);
						when(eventProcessor.getEvent(any(), any(), any(), any())).thenReturn(event);
						var row = mockSelectionResult();

						cut.uploadAttachmentsForCreate(createContext, List.of(attachment));

						verify(eventProcessor).getEvent(eq(CqnService.EVENT_CREATE), eq(testStream), fieldNamesArgumentCaptor.capture(), eq(row));
						verifyFilledFieldNames();
				}
		}

		@Test
		void attachmentAccessExceptionCorrectHandledForCreate() throws AttachmentAccessException {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();
				attachment.setFilename("test.txt");
				attachment.setContent(null);
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).thenReturn(true);
				when(eventProcessor.getEvent(any(), any(), any(), any())).thenReturn(event);
				when(event.processEvent(any(), any(), any(), any(), any(), any())).thenThrow(new AttachmentAccessException());

				List<CdsData> input = List.of(attachment);
				var exception = assertThrows(ServiceException.class, () -> cut.uploadAttachmentsForCreate(createContext, input));

				assertThat(exception.getCause()).isInstanceOf(AttachmentAccessException.class);
		}

		@Test
		void noProcessingNeededForUpdate() {
				CdsEntity entity = mock(CdsEntity.class);
				when(updateContext.getTarget()).thenReturn(entity);
				when(eventProcessor.isAttachmentEvent(entity, List.of(cdsData))).thenReturn(false);

				cut.uploadAttachmentsForUpdate(updateContext, List.of(cdsData));

				verifyNoInteractions(persistenceService);
				verify(eventProcessor).isAttachmentEvent(entity, List.of(cdsData));
		}

		@Test
		void eventProcessorCalledForUpdate() throws IOException {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);

				try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
						var attachment = Attachment.create();
						attachment.setContent(testStream);
						attachment.setId("test");
						mockTargetInContext(serviceEntity.orElseThrow());
						when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).thenReturn(true);
						when(eventProcessor.getEvent(any(), any(), any(), any())).thenReturn(event);
						var row = mockSelectionResult();

						cut.uploadAttachmentsForUpdate(updateContext, List.of(attachment));

						verify(eventProcessor).getEvent(eq(CqnService.EVENT_UPDATE), eq(testStream), fieldNamesArgumentCaptor.capture(), eq(row));
						verifyFilledFieldNames();
				}
		}

		@Test
		void attachmentAccessExceptionCorrectHandledForUpdate() throws AttachmentAccessException {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();
				attachment.setFilename("test.txt");
				attachment.setContent(null);
				attachment.setId("some id");
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).thenReturn(true);
				when(eventProcessor.getEvent(any(), any(), any(), any())).thenReturn(event);
				when(event.processEvent(any(), any(), any(), any(), any(), any())).thenThrow(new AttachmentAccessException());
				mockSelectionResult();

				List<CdsData> input = List.of(attachment);
				var exception = assertThrows(ServiceException.class, () -> cut.uploadAttachmentsForUpdate(updateContext, input));

				assertThat(exception.getCause()).isInstanceOf(AttachmentAccessException.class);
		}

		@Test
		void illegalStateExceptionIfIdNotProvidedForUpdate() throws AttachmentAccessException {
				var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME);
				var attachment = Attachment.create();
				attachment.setFilename("test.txt");
				attachment.setContent(null);
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).thenReturn(true);

				List<CdsData> input = List.of(attachment);
				assertThrows(IllegalStateException.class, () -> cut.uploadAttachmentsForUpdate(updateContext, input));
		}

		@Test
		void noExceptionIfAttachmentEntityWrongDefined() {
				var serviceEntity = runtime.getCdsModel().findEntity(WrongAttachment_.CDS_NAME);
				var attachment = WrongAttachment.create();
				attachment.setFilename("test.txt");
				attachment.setContent(null);
				attachment.setId("test id");
				mockTargetInContext(serviceEntity.orElseThrow());
				when(eventProcessor.isAttachmentEvent(serviceEntity.orElseThrow(), List.of(attachment))).thenReturn(true);
				when(eventProcessor.getEvent(any(), any(), any(), any())).thenReturn(event);
				var row = mockSelectionResult();

				List<CdsData> input = List.of(attachment);
				assertDoesNotThrow(() -> cut.uploadAttachmentsForUpdate(updateContext, input));

				verify(eventProcessor).getEvent(eq(CqnService.EVENT_UPDATE), eq(null), fieldNamesArgumentCaptor.capture(), eq(row));
				verifyEmptyFieldNames();
		}

		private void mockTargetInContext(CdsEntity serviceEntity) {
				when(createContext.getTarget()).thenReturn(serviceEntity);
				when(updateContext.getTarget()).thenReturn(serviceEntity);
		}

		private void verifyFilledFieldNames() {
				//field names taken from model for entity Attachments defined in csn which can be found in AttachmentsHandlerTestBase.CSN_FILE_PATH
				var fieldNames = fieldNamesArgumentCaptor.getValue();
				assertThat(fieldNames.keyField()).isEqualTo("ID");
				assertThat(fieldNames.documentIdField()).isPresent().contains("documentId");
				assertThat(fieldNames.mimeTypeField()).isPresent().contains("mimeType");
				assertThat(fieldNames.fileNameField()).isPresent().contains("filename");
		}

		private void verifyEmptyFieldNames() {
				//field names taken from model for entity WrongAttachments defined in csn which can be found in AttachmentsHandlerTestBase.CSN_FILE_PATH
				var fieldNames = fieldNamesArgumentCaptor.getValue();
				assertThat(fieldNames.keyField()).isEqualTo("ID");
				assertThat(fieldNames.documentIdField()).isEmpty();
				assertThat(fieldNames.mimeTypeField()).isEmpty();
				assertThat(fieldNames.fileNameField()).isEmpty();
		}

		private Row mockSelectionResult() {
				var row = RowImpl.row(cdsData);
				var result = mock(Result.class);
				when(result.single()).thenReturn(row);
				when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);
				return row;
		}

}

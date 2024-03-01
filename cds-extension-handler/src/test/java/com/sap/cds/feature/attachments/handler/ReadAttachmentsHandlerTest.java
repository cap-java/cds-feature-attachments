package com.sap.cds.feature.attachments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.EventItems;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.EventItems_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.WrongAttachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.WrongAttachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.handler.generation.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.DocumentFieldNames;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.model.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier.ItemModifierProvider;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.AttachmentReadEventContext;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;

class ReadAttachmentsHandlerTest {

		private static CdsRuntime runtime;

		private ReadAttachmentsHandler cut;

		private AttachmentService attachmentService;
		private ItemModifierProvider provider;
		private CdsReadEventContext readEventContext;
		private Modifier modifier;
		private ArgumentCaptor<Map> fieldNamesArgumentCaptor;
		private ArgumentCaptor<AttachmentReadEventContext> readEventInputCaptor;

		@BeforeAll
		static void classSetup() {
				runtime = RuntimeHelper.runtime;
		}

		@BeforeEach
		void setup() {
				attachmentService = mock(AttachmentService.class);
				provider = mock(ItemModifierProvider.class);
				cut = new ReadAttachmentsHandler(attachmentService, provider);

				readEventContext = mock(CdsReadEventContext.class);
				modifier = spy(new Modifier() {
				});
				when(provider.getBeforeReadDocumentIdEnhancer(any())).thenReturn(modifier);
				fieldNamesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
				readEventInputCaptor = ArgumentCaptor.forClass(AttachmentReadEventContext.class);
		}

		@Test
		void fieldNamesCorrectReadWithAssociations() {
				var select = Select.from(RootTable_.class).columns(RootTable_::ID);
				mockEventContext(RootTable_.CDS_NAME, select);

				cut.processBefore(readEventContext);

				verify(provider).getBeforeReadDocumentIdEnhancer(fieldNamesArgumentCaptor.capture());
				verify(modifier).items(any());
				var fields = fieldNamesArgumentCaptor.getValue();
				assertThat(fields).hasSize(2);
				var attachmentFields = (DocumentFieldNames) fields.get("attachments");
				var attachmentTableFields = (DocumentFieldNames) fields.get("attachmentTable");
				assertThat(attachmentFields.contentFieldName()).isEqualTo("content");
				assertThat(attachmentFields.documentIdFieldName()).isEqualTo("documentId");
				assertThat(attachmentTableFields.contentFieldName()).isEqualTo("content");
				assertThat(attachmentTableFields.documentIdFieldName()).isEqualTo("documentId");
		}

		@Test
		void fieldNamesCorrectReadWithoutAssociations() {
				var select = Select.from(Attachment_.class).columns(Attachment_::ID);
				mockEventContext(Attachment_.CDS_NAME, select);

				cut.processBefore(readEventContext);

				verify(provider).getBeforeReadDocumentIdEnhancer(fieldNamesArgumentCaptor.capture());
				verify(modifier).items(any());
				var fields = fieldNamesArgumentCaptor.getValue();
				assertThat(fields).hasSize(1);
				var attachmentFields = (DocumentFieldNames) fields.get("");
				assertThat(attachmentFields.contentFieldName()).isEqualTo("content");
				assertThat(attachmentFields.documentIdFieldName()).isEqualTo("documentId");
		}

		@Test
		void noFieldNamesFound() {
				var select = Select.from(EventItems_.class).columns(EventItems_::note);
				mockEventContext(EventItems_.CDS_NAME, select);

				cut.processBefore(readEventContext);

				verifyNoInteractions(provider);
				verifyNoInteractions(modifier);
		}

		@Test
		void noFieldNamesFoundForWrongAttachment() {
				var select = Select.from(WrongAttachment_.class).columns(WrongAttachment_::content);
				mockEventContext(WrongAttachment_.CDS_NAME, select);

				cut.processBefore(readEventContext);

				verifyNoInteractions(provider);
				verifyNoInteractions(modifier);
		}

		@Test
		void dataFilledWithDeepStructure() throws IOException {

				var testString = "test";
				try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {

						var attachmentWithNullValueContent = Attachment.create();
						attachmentWithNullValueContent.setDocumentId("some ID");
						attachmentWithNullValueContent.setContent(null);
						var item1 = Items.create();
						item1.setId("item id1");
						item1.setAttachments(List.of(attachmentWithNullValueContent));
						var attachmentWithoutContentField = Attachment.create();
						attachmentWithoutContentField.setDocumentId("some ID");
						var item2 = Items.create();
						item2.setId("item id2");
						item2.setAttachments(List.of(attachmentWithoutContentField));
						var item3 = Items.create();
						item3.setId("item id3");
						var attachmentWithStreamAsContent = Attachment.create();
						attachmentWithStreamAsContent.setDocumentId("some ID");
						attachmentWithStreamAsContent.setContent(testStream);
						var item4 = Items.create();
						item4.setId("item id4");
						item4.setAttachments(List.of(attachmentWithStreamAsContent));
						var attachmentWithStreamContentButWithoutDocumentId = Attachment.create();
						attachmentWithStreamContentButWithoutDocumentId.setContent(null);
						var item5 = Items.create();
						item5.setId("item id4");
						item5.setAttachments(List.of(attachmentWithStreamContentButWithoutDocumentId));
						var root1 = RootTable.create();
						root1.setItems(List.of(item2, item1, item4, item5));
						var root2 = RootTable.create();
						root2.setItems(List.of(item3));

						var select = Select.from(RootTable_.class);
						mockEventContext(RootTable_.CDS_NAME, select);

						cut.processAfter(readEventContext, List.of(root1, root2));

						assertThat(attachmentWithNullValueContent.getContent()).isInstanceOf(LazyProxyInputStream.class);
						assertThat(attachmentWithoutContentField.getContent()).isNull();
						assertThat(attachmentWithStreamAsContent.getContent()).isEqualTo(testStream);
						assertThat(attachmentWithStreamContentButWithoutDocumentId.getContent()).isNull();
						verifyNoInteractions(attachmentService);
				}
		}

		@Test
		void setAttachmentServiceCalled() throws IOException {
				mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));

				var testString = "test";
				try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
						when(attachmentService.readAttachment(any())).thenReturn(testStream);
						var attachment = Attachment.create();
						attachment.setDocumentId("some ID");
						attachment.setContent(null);

						cut.processAfter(readEventContext, List.of(attachment));

						assertThat(attachment.getContent()).isInstanceOf(LazyProxyInputStream.class);
						verifyNoInteractions(attachmentService);
						byte[] bytes = attachment.getContent().readAllBytes();
						assertThat(bytes).isEqualTo(testString.getBytes(StandardCharsets.UTF_8));
						verify(attachmentService).readAttachment(readEventInputCaptor.capture());
						var readInput = readEventInputCaptor.getValue();
						assertThat(readInput.getDocumentId()).isEqualTo(attachment.getDocumentId());
				}
		}

		@Test
		void attachmentServiceNotCalledIfNoMediaType() {
				var eventItem = EventItems.create();
				eventItem.setId1("test");
				mockEventContext(EventItems_.CDS_NAME, mock(CqnSelect.class));

				cut.processAfter(readEventContext, List.of(eventItem));

				verifyNoInteractions(attachmentService);
		}

		@Test
		void attachmentServiceNotCalledIfWrongAttachment() {
				var wrongAttachment = WrongAttachment.create();
				wrongAttachment.setId(1);
				wrongAttachment.setContent(null);
				mockEventContext(WrongAttachment_.CDS_NAME, mock(CqnSelect.class));

				cut.processAfter(readEventContext, List.of(wrongAttachment));

				verifyNoInteractions(attachmentService);
		}

		@Test
		void classHasCorrectAnnotation() {
				var readHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

				assertThat(readHandlerAnnotation.type()).containsOnly(ApplicationService.class);
				assertThat(readHandlerAnnotation.value()).containsOnly("*");
		}

		@Test
		void afterMethodAfterHasCorrectAnnotations() throws NoSuchMethodException {
				var method = cut.getClass().getMethod("processAfter", CdsReadEventContext.class, List.class);

				var readAfterAnnotation = method.getAnnotation(After.class);
				var readHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

				assertThat(readAfterAnnotation.event()).containsOnly(CqnService.EVENT_READ);
				assertThat(readHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
		}

		@Test
		void beforeMethodHasCorrectAnnotations() throws NoSuchMethodException {
				var method = cut.getClass().getMethod("processBefore", CdsReadEventContext.class);

				var readBeforeAnnotation = method.getAnnotation(Before.class);
				var readHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

				assertThat(readBeforeAnnotation.event()).containsOnly(CqnService.EVENT_READ);
				assertThat(readHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
		}

		private void mockEventContext(String entityName, CqnSelect select) {
				var serviceEntity = runtime.getCdsModel().findEntity(entityName);
				when(readEventContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
				when(readEventContext.getCdsRuntime()).thenReturn(runtime);
				when(readEventContext.getCqn()).thenReturn(select);
		}

}

package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.EventItems;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.EventItems_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.exception.AttachmentStatusException;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.modifier.ItemModifierProvider;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.stream.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.validator.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
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
	private AttachmentStatusValidator attachmentStatusValidator;
	private CdsReadEventContext readEventContext;
	private Modifier modifier;
	private ArgumentCaptor<List<String>> fieldNamesArgumentCaptor;
	private AsyncMalwareScanExecutor asyncMalwareScanExecutor;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		attachmentService = mock(AttachmentService.class);
		provider = mock(ItemModifierProvider.class);
		attachmentStatusValidator = mock(AttachmentStatusValidator.class);
		asyncMalwareScanExecutor = mock(AsyncMalwareScanExecutor.class);
		cut = new ReadAttachmentsHandler(attachmentService, provider, attachmentStatusValidator, asyncMalwareScanExecutor);

		readEventContext = mock(CdsReadEventContext.class);
		modifier = spy(new Modifier() {
		});
		when(provider.getBeforeReadDocumentIdEnhancer(any())).thenReturn(modifier);
		fieldNamesArgumentCaptor = ArgumentCaptor.forClass(List.class);
	}

	@Test
	void fieldNamesCorrectReadWithAssociations() {
		var select = Select.from(RootTable_.class).columns(RootTable_::ID);
		mockEventContext(RootTable_.CDS_NAME, select);

		cut.processBefore(readEventContext);

		verify(provider).getBeforeReadDocumentIdEnhancer(fieldNamesArgumentCaptor.capture());
		verify(modifier).items(any());
		var fields = fieldNamesArgumentCaptor.getValue();
		assertThat(fields).hasSize(2).contains("attachments").contains("itemAttachments");
	}

	@Test
	void fieldNamesCorrectReadWithoutAssociations() {
		var select = Select.from(Attachment_.class).columns(Attachment_::ID);
		mockEventContext(Attachment_.CDS_NAME, select);

		cut.processBefore(readEventContext);

		verify(provider).getBeforeReadDocumentIdEnhancer(fieldNamesArgumentCaptor.capture());
		verify(modifier).items(any());
		var fields = fieldNamesArgumentCaptor.getValue();
		assertThat(fields).hasSize(1).contains("");
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
	void dataFilledWithDeepStructure() throws IOException {
		var testString = "test";
		try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {

			var attachmentWithNullValueContent = Attachments.create();
			attachmentWithNullValueContent.setContentId("some ID");
			attachmentWithNullValueContent.setContent(null);
			var item1 = Items.create();
			item1.setId("item id1");
			item1.setAttachments(List.of(attachmentWithNullValueContent));
			var attachmentWithoutContentField = Attachments.create();
			attachmentWithoutContentField.setContentId("some ID");
			var item2 = Items.create();
			item2.setId("item id2");
			item2.setAttachments(List.of(attachmentWithoutContentField));
			var item3 = Items.create();
			item3.setId("item id3");
			var attachmentWithStreamAsContent = Attachments.create();
			attachmentWithStreamAsContent.setContentId("some ID");
			attachmentWithStreamAsContent.setContent(testStream);
			var item4 = Items.create();
			item4.setId("item id4");
			item4.setAttachments(List.of(attachmentWithStreamAsContent));
			var attachmentWithStreamContentButWithoutDocumentId = Attachments.create();
			attachmentWithStreamContentButWithoutDocumentId.setContent(mock(InputStream.class));
			var item5 = Items.create();
			item5.setId("item id4");
			item5.setAttachments(List.of(attachmentWithStreamContentButWithoutDocumentId));
			var root1 = RootTable.create();
			root1.setItemTable(List.of(item2, item1, item4, item5));
			var root2 = RootTable.create();
			root2.setItemTable(List.of(item3));

			var select = Select.from(RootTable_.class);
			mockEventContext(RootTable_.CDS_NAME, select);

			cut.processAfter(readEventContext, List.of(root1, root2));

			assertThat(attachmentWithNullValueContent.getContent()).isInstanceOf(LazyProxyInputStream.class);
			assertThat(attachmentWithoutContentField.getContent()).isNull();
			assertThat(attachmentWithStreamAsContent.getContent()).isInstanceOf(LazyProxyInputStream.class);
			assertThat(attachmentWithStreamContentButWithoutDocumentId.getContent()).isInstanceOf(LazyProxyInputStream.class);
			verifyNoInteractions(attachmentService);
		}
	}

	@Test
	void setAttachmentServiceCalled() throws IOException {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));

		var testString = "test";
		try (var testStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8))) {
			when(attachmentService.readAttachment(any())).thenReturn(testStream);
			var attachment = Attachments.create();
			attachment.setContentId("some ID");
			attachment.setContent(null);
			attachment.setStatus(StatusCode.CLEAN);

			cut.processAfter(readEventContext, List.of(attachment));

			assertThat(attachment.getContent()).isInstanceOf(LazyProxyInputStream.class);
			verifyNoInteractions(attachmentService);
			byte[] bytes = attachment.getContent().readAllBytes();
			assertThat(bytes).isEqualTo(testString.getBytes(StandardCharsets.UTF_8));
			verify(attachmentService).readAttachment(attachment.getContentId());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.INFECTED, StatusCode.UNSCANNED})
	@EmptySource
	@NullSource
	void wrongStatusThrowsException(String status) {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setContentId("some ID");
		attachment.setContent(null);
		attachment.setStatus(status);
		doThrow(AttachmentStatusException.class).when(attachmentStatusValidator).verifyStatus(status);

		List<CdsData> attachments = List.of(attachment);
		assertThrows(AttachmentStatusException.class, () -> cut.processAfter(readEventContext, attachments));
	}

	@ParameterizedTest
	@ValueSource(strings = {StatusCode.INFECTED, StatusCode.UNSCANNED})
	@EmptySource
	@NullSource
	void wrongStatusThrowsExceptionDuringContentRead(String status) {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setContentId("some ID");
		attachment.setContent(null);
		attachment.setStatus(status);

		assertDoesNotThrow(() -> cut.processAfter(readEventContext, List.of(attachment)));

		doThrow(AttachmentStatusException.class).when(attachmentStatusValidator).verifyStatus(status);
		var content = attachment.getContent();
		assertThat(content).isInstanceOf(LazyProxyInputStream.class);
		verifyNoInteractions(attachmentService);
		assertThrows(AttachmentStatusException.class, content::readAllBytes);
	}

	@Test
	void scannerCalledForUnscannedAttachments() {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setContentId("some ID");
		attachment.setContent(mock(InputStream.class));
		attachment.setStatus(StatusCode.UNSCANNED);

		cut.processAfter(readEventContext, List.of(attachment));

		verify(asyncMalwareScanExecutor).scanAsync(readEventContext.getTarget(), attachment.getContentId());
	}

	@Test
	void scannerNotCalledForUnscannedAttachmentsIfNoContentProvided() {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setContentId("some ID");
		attachment.setContent(null);
		attachment.setStatus(StatusCode.UNSCANNED);

		cut.processAfter(readEventContext, List.of(attachment));

		verifyNoInteractions(asyncMalwareScanExecutor);
	}


	@Test
	void scannerNotCalledForInfectedAttachments() {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setContentId("some ID");
		attachment.setContent(null);
		attachment.setStatus(StatusCode.INFECTED);

		cut.processAfter(readEventContext, List.of(attachment));

		verifyNoInteractions(asyncMalwareScanExecutor);
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

	@Test
	void statusNotVerifiedIfNotOnlyContentIsRequested() {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setContentId("some ID");
		attachment.setContent(mock(InputStream.class));
		attachment.setStatus(StatusCode.INFECTED);
		attachment.setId(UUID.randomUUID().toString());

		cut.processAfter(readEventContext, List.of(attachment));

		verifyNoInteractions(attachmentStatusValidator);
	}

	@Test
	void emptyDocumentIdAndEmptyContentReturnNullContent() {
		mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
		var attachment = Attachments.create();
		attachment.setStatus(StatusCode.INFECTED);
		attachment.setContent(null);

		cut.processAfter(readEventContext, List.of(attachment));

		verifyNoInteractions(attachmentStatusValidator);
		assertThat(attachment.getContent()).isNull();
	}

	private void mockEventContext(String entityName, CqnSelect select) {
		var serviceEntity = runtime.getCdsModel().findEntity(entityName);
		when(readEventContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
		when(readEventContext.getModel()).thenReturn(runtime.getCdsModel());
		when(readEventContext.getCqn()).thenReturn(select);
	}

}

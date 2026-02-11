/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.EventItems;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.EventItems_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusException;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;
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

class ReadAttachmentsHandlerTest {

  private static CdsRuntime runtime;

  private ApplicationServiceAttachmentsHandler cut;

  private AttachmentService attachmentService;
  private AttachmentStatusValidator attachmentStatusValidator;
  private CdsReadEventContext readEventContext;
  private AsyncMalwareScanExecutor asyncMalwareScanExecutor;

  // Additional mocks required for ApplicationServiceAttachmentsHandler
  private ModifyAttachmentEventFactory eventFactory;
  private ThreadDataStorageReader storageReader;
  private AttachmentsReader attachmentsReader;
  private AttachmentService outboxedAttachmentService;
  private MarkAsDeletedAttachmentEvent deleteEvent;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    attachmentService = mock(AttachmentService.class);
    attachmentStatusValidator = mock(AttachmentStatusValidator.class);
    asyncMalwareScanExecutor = mock(AsyncMalwareScanExecutor.class);
    eventFactory = mock(ModifyAttachmentEventFactory.class);
    storageReader = mock(ThreadDataStorageReader.class);
    attachmentsReader = mock(AttachmentsReader.class);
    outboxedAttachmentService = mock(AttachmentService.class);
    deleteEvent = mock(MarkAsDeletedAttachmentEvent.class);

    cut =
        new ApplicationServiceAttachmentsHandler(
            eventFactory,
            storageReader,
            ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER,
            attachmentService,
            attachmentStatusValidator,
            asyncMalwareScanExecutor,
            attachmentsReader,
            outboxedAttachmentService,
            deleteEvent);

    readEventContext = mock(CdsReadEventContext.class);
  }

  @Test
  void fieldNamesCorrectReadWithAssociations() {
    var select = Select.from(RootTable_.class).columns(RootTable_::ID);
    mockEventContext(RootTable_.CDS_NAME, select);

    cut.processBeforeRead(readEventContext);
  }

  @Test
  void fieldNamesCorrectReadWithoutAssociations() {
    var select = Select.from(Attachment_.class).columns(Attachment_::ID);
    mockEventContext(Attachment_.CDS_NAME, select);

    cut.processBeforeRead(readEventContext);
  }

  @Test
  void noFieldNamesFound() {
    var select = Select.from(EventItems_.class).columns(EventItems_::note);
    mockEventContext(EventItems_.CDS_NAME, select);

    cut.processBeforeRead(readEventContext);
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
      var attachmentWithStreamContentButWithoutContentId = Attachments.create();
      var inputMock = mock(InputStream.class);
      attachmentWithStreamContentButWithoutContentId.setContent(inputMock);
      var item5 = Items.create();
      item5.setId("item id5");
      item5.setAttachments(List.of(attachmentWithStreamContentButWithoutContentId));
      var root1 = RootTable.create();
      root1.setItemTable(List.of(item2, item1, item4, item5));
      var root2 = RootTable.create();
      root2.setItemTable(List.of(item3));

      var select = Select.from(RootTable_.class);
      mockEventContext(RootTable_.CDS_NAME, select);

      cut.processAfterRead(readEventContext, List.of(root1, root2));

      assertThat(attachmentWithNullValueContent.getContent())
          .isInstanceOf(LazyProxyInputStream.class);
      assertThat(attachmentWithoutContentField.getContent()).isNull();
      assertThat(attachmentWithStreamAsContent.getContent())
          .isInstanceOf(LazyProxyInputStream.class);
      assertThat(attachmentWithStreamContentButWithoutContentId.getContent()).isEqualTo(inputMock);
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

      cut.processAfterRead(readEventContext, List.of(attachment));

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
    assertThrows(
        AttachmentStatusException.class, () -> cut.processAfterRead(readEventContext, attachments));
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

    assertDoesNotThrow(() -> cut.processAfterRead(readEventContext, List.of(attachment)));

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

    cut.processAfterRead(readEventContext, List.of(attachment));

    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId());
  }

  @Test
  void scannerCalledForUnscannedAttachmentsIfNoContentProvided() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(null);
    attachment.setStatus(StatusCode.UNSCANNED);

    cut.processAfterRead(readEventContext, List.of(attachment));

    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId());
  }

  @Test
  void scannerNotCalledForInfectedAttachments() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(null);
    attachment.setStatus(StatusCode.INFECTED);

    cut.processAfterRead(readEventContext, List.of(attachment));

    verifyNoInteractions(asyncMalwareScanExecutor);
  }

  @Test
  void attachmentServiceNotCalledIfNoMediaType() {
    var eventItem = EventItems.create();
    eventItem.setId1("test");
    mockEventContext(EventItems_.CDS_NAME, mock(CqnSelect.class));

    cut.processAfterRead(readEventContext, List.of(eventItem));

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
    var method =
        cut.getClass().getDeclaredMethod("processAfterRead", CdsReadEventContext.class, List.class);

    var readAfterAnnotation = method.getAnnotation(After.class);
    var readHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(readAfterAnnotation.event()).containsOnly(CqnService.EVENT_READ);
    assertThat(readHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.EARLY);
  }

  @Test
  void beforeMethodHasCorrectAnnotations() throws NoSuchMethodException {
    var method = cut.getClass().getDeclaredMethod("processBeforeRead", CdsReadEventContext.class);

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

    cut.processAfterRead(readEventContext, List.of(attachment));

    verifyNoInteractions(attachmentStatusValidator);
  }

  @Test
  void emptyContentIdAndEmptyContentReturnNullContent() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setStatus(StatusCode.INFECTED);
    attachment.setContent(null);

    cut.processAfterRead(readEventContext, List.of(attachment));

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

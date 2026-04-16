/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnlyTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnlyTable_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ExtendedErrorStatuses;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation.AttachmentValidationHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.CountingInputStream;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class CreateAttachmentsHandlerTest {

  private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";
  private static CdsRuntime runtime;

  private CreateAttachmentsHandler cut;
  private ModifyAttachmentEventFactory eventFactory;
  private CdsCreateEventContext createContext;
  private ModifyAttachmentEvent event;
  private ThreadDataStorageReader storageReader;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    eventFactory = mock(ModifyAttachmentEventFactory.class);
    storageReader = mock(ThreadDataStorageReader.class);
    cut =
        new CreateAttachmentsHandler(
            eventFactory,
            storageReader,
            ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER,
            runtime);

    createContext = mock(CdsCreateEventContext.class);
    event = mock(ModifyAttachmentEvent.class);

    ParameterInfo parameterInfo = mock(ParameterInfo.class);
    when(createContext.getParameterInfo()).thenReturn(parameterInfo);
  }

  @Test
  void noContentInDataNothingToDo() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    var attachment = Attachments.create();

    cut.processBefore(createContext, List.of(attachment));

    verifyNoInteractions(eventFactory);
  }

  @Test
  void idsAreSetInDataForCreate() {
    getEntityAndMockContext(RootTable_.CDS_NAME);
    var roots = RootTable.create();
    var attachment = Attachments.create();
    attachment.setFileName("test.txt");
    attachment.setContent(null);
    attachment.put("up__ID", "test");
    roots.setAttachments(List.of(attachment));
    when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

    cut.processBefore(createContext, List.of(roots));

    verify(eventFactory).getEvent(null, null, Attachments.create());
  }

  @Test
  void eventProcessorCalledForCreate() throws IOException {
    getEntityAndMockContext(Attachment_.CDS_NAME);

    try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
      var attachment = Attachments.create();
      attachment.setContent(testStream);
      when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

      cut.processBefore(createContext, List.of(attachment));

      ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
      verify(eventFactory)
          .getEvent(streamCaptor.capture(), eq((String) null), eq(Attachments.create()));
      InputStream captured = streamCaptor.getValue();
      assertThat(captured).isInstanceOf(CountingInputStream.class);
      assertThat(((CountingInputStream) captured).getDelegate()).isSameAs(testStream);
    }
  }

  @Test
  void readonlyDataFilledForDraftActivate() {
    getEntityAndMockContext(Attachment_.CDS_NAME);

    var attachment = Attachments.create();
    attachment.setContentId("Document Id");
    attachment.setStatus("Status Code");
    attachment.setScannedAt(Instant.now());
    attachment.setContent(null);
    when(storageReader.get()).thenReturn(true);

    cut.processBeforeForDraft(createContext, List.of(attachment));

    verifyNoInteractions(eventFactory, event);
    assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNotNull();
    var readOnlyData = (CdsData) attachment.get(DRAFT_READONLY_CONTEXT);
    assertThat(readOnlyData)
        .containsEntry(Attachment.CONTENT_ID, attachment.getContentId())
        .containsEntry(Attachment.STATUS, attachment.getStatus())
        .containsEntry(Attachment.SCANNED_AT, attachment.getScannedAt());
  }

  @Test
  void readonlyDataClearedIfNotDraftActivate() {
    getEntityAndMockContext(Attachment_.CDS_NAME);

    var createAttachment = Attachments.create();
    var contentId = "Document Id";
    createAttachment.setContentId(contentId);
    createAttachment.setContent(null);
    var readonlyData = CdsData.create();
    readonlyData.put(Attachment.STATUS, "some wrong status code");
    readonlyData.put(Attachment.CONTENT_ID, "some other document id");
    readonlyData.put(Attachment.SCANNED_AT, Instant.EPOCH);
    createAttachment.put(DRAFT_READONLY_CONTEXT, readonlyData);
    when(storageReader.get()).thenReturn(false);

    cut.processBeforeForDraft(createContext, List.of(createAttachment));

    verifyNoInteractions(eventFactory, event);
    assertThat(createAttachment.get(DRAFT_READONLY_CONTEXT)).isNull();
    assertThat(createAttachment)
        .containsEntry(Attachment.CONTENT_ID, contentId)
        .doesNotContainKey(Attachment.STATUS)
        .doesNotContainKey(Attachment.SCANNED_AT);
  }

  @Test
  void readonlyDataNotFilledForNonDraftActivate() {
    getEntityAndMockContext(Attachment_.CDS_NAME);

    var attachment = Attachments.create();
    attachment.setContentId("Document Id");
    attachment.setStatus("Status Code");
    attachment.setScannedAt(Instant.now());
    when(storageReader.get()).thenReturn(false);

    cut.processBeforeForDraft(createContext, List.of(attachment));

    verifyNoInteractions(eventFactory, event);
    assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNull();
  }

  @Test
  void eventProcessorNotCalledForCreateForDraft() throws IOException {
    getEntityAndMockContext(Attachment_.CDS_NAME);

    try (var testStream = new ByteArrayInputStream("testString".getBytes(StandardCharsets.UTF_8))) {
      var attachment = Attachments.create();
      attachment.setContent(testStream);
      when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
      when(createContext.getService()).thenReturn(mock(ApplicationService.class));

      cut.processBeforeForDraft(createContext, List.of(attachment));

      verifyNoInteractions(eventFactory);
    }
  }

  @Test
  void attachmentAccessExceptionCorrectHandledForCreate() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    var attachment = Attachments.create();
    attachment.setFileName("test.txt");
    attachment.setContent(null);
    when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
    when(event.processEvent(any(), any(), any(), any())).thenThrow(new ServiceException(""));

    List<CdsData> input = List.of(attachment);
    assertThrows(ServiceException.class, () -> cut.processBefore(createContext, input));
  }

  @Test
  void handlerCalledForMediaEventInAssociationIdsAreSet() {
    getEntityAndMockContext(Events_.CDS_NAME);
    var events = Events.create();
    events.setContent("test");
    var items = Items.create();
    var attachment = Attachments.create();
    var content = mock(InputStream.class);
    attachment.setContent(content);
    items.setAttachments(List.of(attachment));
    events.setItems(List.of(items));
    when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

    List<CdsData> input = List.of(events);
    cut.processBefore(createContext, input);

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(eventFactory).getEvent(streamCaptor.capture(), any(), any());
    InputStream captured = streamCaptor.getValue();
    assertThat(captured).isInstanceOf(CountingInputStream.class);
    assertThat(((CountingInputStream) captured).getDelegate()).isSameAs(content);
  }

  @Test
  void readonlyFieldsAreUsedFromOwnContext() {
    getEntityAndMockContext(Attachment_.CDS_NAME);

    var readonlyFields = CdsData.create();
    readonlyFields.put(Attachment.CONTENT_ID, "Document Id");
    readonlyFields.put(Attachment.STATUS, "Status Code");
    readonlyFields.put(Attachment.SCANNED_AT, Instant.now());
    var testStream = mock(InputStream.class);
    var attachment = Attachments.create();
    attachment.setContent(testStream);
    attachment.put("DRAFT_READONLY_CONTEXT", readonlyFields);

    when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

    cut.processBefore(createContext, List.of(attachment));

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(eventFactory)
        .getEvent(
            streamCaptor.capture(),
            eq((String) readonlyFields.get(Attachment.CONTENT_ID)),
            eq(Attachments.create()));
    InputStream captured = streamCaptor.getValue();
    assertThat(captured).isInstanceOf(CountingInputStream.class);
    assertThat(((CountingInputStream) captured).getDelegate()).isSameAs(testStream);
    assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNull();
    assertThat(attachment.getContentId()).isEqualTo(readonlyFields.get(Attachment.CONTENT_ID));
    assertThat(attachment.getStatus()).isEqualTo(readonlyFields.get(Attachment.STATUS));
    assertThat(attachment.getScannedAt()).isEqualTo(readonlyFields.get(Attachment.SCANNED_AT));
  }

  @Test
  void handlerCalledForNonMediaEventNothingSetAndCalled() {
    getEntityAndMockContext(Events_.CDS_NAME);
    var events = Events.create();
    events.setContent("test");
    var eventItems = Items.create();
    var attachment = Attachments.create();
    attachment.setContent(mock(InputStream.class));
    eventItems.setAttachments(List.of(attachment));
    events.setEventItems(List.of(eventItems));
    when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

    List<CdsData> input = List.of(events);
    cut.processBefore(createContext, input);

    verifyNoInteractions(eventFactory);
    assertThat(events.getId1()).isNull();
    assertThat(events.getId2()).isNull();
  }

  @Test
  void restoreError_proceedsSuccessfully_noException() {
    var context = mock(EventContext.class);
    doNothing().when(context).proceed();

    assertDoesNotThrow(() -> cut.restoreError(context));
    verify(context).proceed();
  }

  @Test
  void restoreError_contentTooLargeWithMaxSize_throwsWithMaxSize() {
    var context = mock(EventContext.class);
    var originalException =
        new ServiceException(ExtendedErrorStatuses.CONTENT_TOO_LARGE, "original message");
    doThrow(originalException).when(context).proceed();
    when(context.get("attachment.MaxSize")).thenReturn("10MB");

    var exception = assertThrows(ServiceException.class, () -> cut.restoreError(context));

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    assertThat(exception.getMessage()).contains("File size exceeds the limit of 10MB.");
    assertThat(exception).isNotSameAs(originalException);
  }

  @Test
  void restoreError_contentTooLargeWithoutMaxSize_throwsWithoutMaxSize() {
    var context = mock(EventContext.class);
    var originalException =
        new ServiceException(ExtendedErrorStatuses.CONTENT_TOO_LARGE, "original message");
    doThrow(originalException).when(context).proceed();
    when(context.get("attachment.MaxSize")).thenReturn(null);

    var exception = assertThrows(ServiceException.class, () -> cut.restoreError(context));

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    assertThat(exception.getMessage()).contains("File size exceeds the limit.");
    assertThat(exception).isNotSameAs(originalException);
  }

  @Test
  void restoreError_otherServiceException_rethrowsOriginal() {
    var context = mock(EventContext.class);
    var originalException = new ServiceException(ErrorStatuses.BAD_REQUEST, "some other error");
    doThrow(originalException).when(context).proceed();

    var exception = assertThrows(ServiceException.class, () -> cut.restoreError(context));

    assertThat(exception).isSameAs(originalException);
  }

  @Test
  void processBeforeForMetadata_executesValidation() {
    EventContext context = mock(EventContext.class);
    CdsEntity entity = mock(CdsEntity.class);
    List<CdsData> data = List.of(mock(CdsData.class));
    when(context.getTarget()).thenReturn(entity);

    try (MockedStatic<AttachmentValidationHelper> helper =
        mockStatic(AttachmentValidationHelper.class)) {
      helper
          .when(() -> AttachmentValidationHelper.validateMediaAttachments(entity, data, runtime))
          .thenAnswer(invocation -> null);
      // when
      new CreateAttachmentsHandler(eventFactory, storageReader, "400MB", runtime)
          .processBeforeForMetadata(context, data);
      // then
      helper.verify(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, data, runtime));
    }
  }

  @Test
  void inlineAttachmentContentTriggersEventProcessing() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    var testStream = mock(InputStream.class);
    row.setAvatarContent(testStream);

    cut.processBefore(createContext, List.of(row));

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(eventFactory)
        .processInlineEvent(
            streamCaptor.capture(), eq((String) null), any(), eq(createContext), any(), any());
    InputStream captured = streamCaptor.getValue();
    assertThat(captured).isInstanceOf(CountingInputStream.class);
    assertThat(((CountingInputStream) captured).getDelegate()).isSameAs(testStream);
  }

  @Test
  void inlineAttachmentWithNoContentNoProcessing() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    row.setTitle("just a title");

    cut.processBefore(createContext, List.of(row));

    verifyNoInteractions(eventFactory);
  }

  @Test
  void inlineAttachmentWithNullContentValueProcessed() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    row.put(InlineOnlyTable.AVATAR_CONTENT, null);

    cut.processBefore(createContext, List.of(row));

    verify(eventFactory)
        .processInlineEvent(eq(null), eq((String) null), any(), eq(createContext), any(), any());
  }

  @Test
  void inlineAttachmentContentLengthExceedsLimitThrows() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    row.setAvatarContent(mock(InputStream.class));
    ParameterInfo paramInfo = mock(ParameterInfo.class);
    when(paramInfo.getHeader("Content-Length")).thenReturn("999999999999");
    when(createContext.getParameterInfo()).thenReturn(paramInfo);

    List<CdsData> data = List.of(row);
    assertThrows(ServiceException.class, () -> cut.processBefore(createContext, data));
  }

  @Test
  void inlineAttachmentInvalidContentLengthThrows() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    row.setAvatarContent(mock(InputStream.class));
    ParameterInfo paramInfo = mock(ParameterInfo.class);
    when(paramInfo.getHeader("Content-Length")).thenReturn("not-a-number");
    when(createContext.getParameterInfo()).thenReturn(paramInfo);

    List<CdsData> data = List.of(row);
    assertThrows(ServiceException.class, () -> cut.processBefore(createContext, data));
  }

  @Test
  void inlineAttachmentLimitExceededDuringProcessingThrows() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    var testStream = mock(InputStream.class);
    row.setAvatarContent(testStream);
    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("test"));

    List<CdsData> data = List.of(row);
    var exception =
        assertThrows(RuntimeException.class, () -> cut.processBefore(createContext, data));
    assertThat(exception.getMessage()).isEqualTo("test");
  }

  @Test
  void inlineAttachmentContentLengthWithinLimitProcesses() {
    getEntityAndMockContext(InlineOnlyTable_.CDS_NAME);
    var row = InlineOnlyTable.create();
    row.setId("test-id");
    var testStream = mock(InputStream.class);
    row.setAvatarContent(testStream);
    ParameterInfo paramInfo = mock(ParameterInfo.class);
    when(paramInfo.getHeader("Content-Length")).thenReturn("100");
    when(createContext.getParameterInfo()).thenReturn(paramInfo);

    cut.processBefore(createContext, List.of(row));

    verify(eventFactory).processInlineEvent(any(), any(), any(), eq(createContext), any(), any());
  }

  private void getEntityAndMockContext(String cdsName) {
    var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
    mockTargetInCreateContext(serviceEntity.orElseThrow());
  }

  private void mockTargetInCreateContext(CdsEntity serviceEntity) {
    when(createContext.getTarget()).thenReturn(serviceEntity);
  }
}

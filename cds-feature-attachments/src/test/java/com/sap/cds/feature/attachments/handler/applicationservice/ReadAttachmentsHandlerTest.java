/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnly_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusException;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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

  private ReadAttachmentsHandler cut;

  private AttachmentService attachmentService;
  private AttachmentStatusValidator attachmentStatusValidator;
  private CdsReadEventContext readEventContext;
  private AsyncMalwareScanExecutor asyncMalwareScanExecutor;
  private PersistenceService persistenceService;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    attachmentService = mock(AttachmentService.class);
    attachmentStatusValidator = mock(AttachmentStatusValidator.class);
    asyncMalwareScanExecutor = mock(AsyncMalwareScanExecutor.class);
    persistenceService = mock(PersistenceService.class);
    cut =
        new ReadAttachmentsHandler(
            attachmentService,
            attachmentStatusValidator,
            asyncMalwareScanExecutor,
            persistenceService,
            new AssociationCascader(),
            true);

    readEventContext = mock(CdsReadEventContext.class);
  }

  @Test
  void fieldNamesCorrectReadWithAssociations() {
    var select = Select.from(RootTable_.class).columns(RootTable_::ID);
    mockEventContext(RootTable_.CDS_NAME, select);

    cut.processBefore(readEventContext);
  }

  @Test
  void fieldNamesCorrectReadWithoutAssociations() {
    var select = Select.from(Attachment_.class).columns(Attachment_::ID);
    mockEventContext(Attachment_.CDS_NAME, select);

    cut.processBefore(readEventContext);
  }

  @Test
  void noFieldNamesFound() {
    var select = Select.from(EventItems_.class).columns(EventItems_::note);
    mockEventContext(EventItems_.CDS_NAME, select);

    cut.processBefore(readEventContext);
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

      cut.processAfter(readEventContext, List.of(root1, root2));

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
      attachment.setScannedAt(Instant.now());

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
    assertThrows(
        AttachmentStatusException.class, () -> cut.processAfter(readEventContext, attachments));
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

    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId(), Optional.empty());
  }

  @Test
  void scannerCalledForUnscannedAttachmentsIfNoContentProvided() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(null);
    attachment.setStatus(StatusCode.UNSCANNED);

    cut.processAfter(readEventContext, List.of(attachment));

    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId(), Optional.empty());
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
  void scannerCalledForStaleCleanAttachment() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.CLEAN);
    attachment.setScannedAt(Instant.now().minus(4, ChronoUnit.DAYS));
    doThrow(AttachmentStatusException.class)
        .when(attachmentStatusValidator)
        .verifyStatus(StatusCode.SCANNING);

    List<CdsData> attachments = List.of(attachment);
    assertThrows(
        AttachmentStatusException.class, () -> cut.processAfter(readEventContext, attachments));

    verify(persistenceService).run(any(com.sap.cds.ql.cqn.CqnUpdate.class));
    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId(), Optional.empty());
    assertThat(attachment.getStatus()).isEqualTo(StatusCode.SCANNING);
  }

  @Test
  void scannerCalledForCleanAttachmentWithNullScannedAt() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.CLEAN);
    attachment.setScannedAt(null);
    doThrow(AttachmentStatusException.class)
        .when(attachmentStatusValidator)
        .verifyStatus(StatusCode.SCANNING);

    List<CdsData> attachments = List.of(attachment);
    assertThrows(
        AttachmentStatusException.class, () -> cut.processAfter(readEventContext, attachments));

    verify(persistenceService).run(any(com.sap.cds.ql.cqn.CqnUpdate.class));
    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId(), Optional.empty());
    assertThat(attachment.getStatus()).isEqualTo(StatusCode.SCANNING);
  }

  @Test
  void scannerNotCalledForFreshCleanAttachment() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.CLEAN);
    attachment.setScannedAt(Instant.now().minus(1, ChronoUnit.DAYS));

    cut.processAfter(readEventContext, List.of(attachment));

    verifyNoInteractions(asyncMalwareScanExecutor);
    verifyNoInteractions(persistenceService);
    assertThat(attachment.getStatus()).isEqualTo(StatusCode.CLEAN);
  }

  @Test
  void scannerNotCalledForCleanAttachmentScannedExactlyAtThreshold() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.CLEAN);
    attachment.setScannedAt(
        Instant.now().minus(ReadAttachmentsHandler.RESCAN_THRESHOLD).plusSeconds(60));

    cut.processAfter(readEventContext, List.of(attachment));

    verifyNoInteractions(asyncMalwareScanExecutor);
    verifyNoInteractions(persistenceService);
  }

  @Test
  void persistenceServiceNotCalledForUnscannedAttachments() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.UNSCANNED);

    cut.processAfter(readEventContext, List.of(attachment));

    verify(asyncMalwareScanExecutor)
        .scanAsync(readEventContext.getTarget(), attachment.getContentId(), Optional.empty());
    verify(attachmentStatusValidator).verifyStatus(StatusCode.UNSCANNED);
    verifyNoInteractions(persistenceService);
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
  void emptyContentIdAndEmptyContentReturnNullContent() {
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setStatus(StatusCode.INFECTED);
    attachment.setContent(null);

    cut.processAfter(readEventContext, List.of(attachment));

    verifyNoInteractions(attachmentStatusValidator);
    assertThat(attachment.getContent()).isNull();
  }

  @Test
  void scannerNotAvailable_staleCleanAttachmentIsNotRescanned() {
    var handlerWithoutScanner =
        new ReadAttachmentsHandler(
            attachmentService,
            attachmentStatusValidator,
            asyncMalwareScanExecutor,
            persistenceService,
            new AssociationCascader(),
            false);
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.CLEAN);
    attachment.setScannedAt(Instant.now().minus(4, ChronoUnit.DAYS));

    handlerWithoutScanner.processAfter(readEventContext, List.of(attachment));

    verifyNoInteractions(asyncMalwareScanExecutor);
    verifyNoInteractions(persistenceService);
    assertThat(attachment.getStatus()).isEqualTo(StatusCode.CLEAN);
  }

  @Test
  void scannerNotAvailable_cleanAttachmentWithNullScannedAtIsNotRescanned() {
    var handlerWithoutScanner =
        new ReadAttachmentsHandler(
            attachmentService,
            attachmentStatusValidator,
            asyncMalwareScanExecutor,
            persistenceService,
            new AssociationCascader(),
            false);
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.CLEAN);
    attachment.setScannedAt(null);

    handlerWithoutScanner.processAfter(readEventContext, List.of(attachment));

    verifyNoInteractions(asyncMalwareScanExecutor);
    verifyNoInteractions(persistenceService);
    assertThat(attachment.getStatus()).isEqualTo(StatusCode.CLEAN);
  }

  @Test
  void scannerNotAvailable_unscannedAttachmentStillFailsValidation() {
    var handlerWithoutScanner =
        new ReadAttachmentsHandler(
            attachmentService,
            attachmentStatusValidator,
            asyncMalwareScanExecutor,
            persistenceService,
            new AssociationCascader(),
            false);
    mockEventContext(Attachment_.CDS_NAME, mock(CqnSelect.class));
    var attachment = Attachments.create();
    attachment.setContentId("some ID");
    attachment.setContent(mock(InputStream.class));
    attachment.setStatus(StatusCode.UNSCANNED);
    doThrow(AttachmentStatusException.class)
        .when(attachmentStatusValidator)
        .verifyStatus(StatusCode.UNSCANNED);

    List<CdsData> attachments = List.of(attachment);
    assertThrows(
        AttachmentStatusException.class,
        () -> handlerWithoutScanner.processAfter(readEventContext, attachments));

    verifyNoInteractions(asyncMalwareScanExecutor);
    verifyNoInteractions(persistenceService);
  }

  private void mockEventContext(String entityName, CqnSelect select) {
    var serviceEntity = runtime.getCdsModel().findEntity(entityName);
    when(readEventContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
    when(readEventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(readEventContext.getCqn()).thenReturn(select);
  }

  // --- Inline Attachment Tests ---

  @Test
  void inlineContentWrappedWithLazyProxyOnRead() {
    mockEventContext(RootTable_.CDS_NAME, mock(CqnSelect.class));

    // Create root data with inline attachment fields
    var root = CdsData.create();
    root.put("ID", UUID.randomUUID().toString());
    root.put("profilePicture_content", null);
    root.put("profilePicture_contentId", "inline-doc-1");
    root.put("profilePicture_status", StatusCode.CLEAN);

    cut.processAfter(readEventContext, List.of(root));

    assertThat(root.get("profilePicture_content")).isInstanceOf(LazyProxyInputStream.class);
  }

  @Test
  void inlineContentWithoutContentIdRemainsNull() {
    mockEventContext(RootTable_.CDS_NAME, mock(CqnSelect.class));

    var root = CdsData.create();
    root.put("ID", UUID.randomUUID().toString());
    root.put("profilePicture_content", null);
    // No contentId — should not be wrapped

    cut.processAfter(readEventContext, List.of(root));

    assertThat(root.get("profilePicture_content")).isNull();
  }

  @Test
  void inlineContentWithExistingStreamWrappedWithProxy() throws IOException {
    mockEventContext(RootTable_.CDS_NAME, mock(CqnSelect.class));
    var testContent = "inline photo bytes";
    var testStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8));

    var root = CdsData.create();
    root.put("ID", UUID.randomUUID().toString());
    root.put("profilePicture_content", testStream);
    root.put("profilePicture_contentId", "inline-doc-2");
    root.put("profilePicture_status", StatusCode.CLEAN);

    cut.processAfter(readEventContext, List.of(root));

    assertThat(root.get("profilePicture_content")).isInstanceOf(LazyProxyInputStream.class);
    // The proxy uses the existing stream supplier
    byte[] bytes = ((InputStream) root.get("profilePicture_content")).readAllBytes();
    assertThat(bytes).isEqualTo(testContent.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void processBeforeWithOnlyInlineAttachmentsModifiesCqn() {
    // InlineOnly entity has inline attachment but NO composition associations
    // This covers the branch: fieldNames.isEmpty() && !inlinePrefixes.isEmpty()
    var select = Select.from(InlineOnly_.class).columns(InlineOnly_::ID);
    mockEventContext(InlineOnly_.CDS_NAME, select);

    cut.processBefore(readEventContext);

    // Verify CQN was modified (setCqn called)
    verify(readEventContext).setCqn(any(CqnSelect.class));
  }

  @Test
  void processAfterWithInlineOnlyEntityWrapsContent() {
    mockEventContext(InlineOnly_.CDS_NAME, mock(CqnSelect.class));

    var root = CdsData.create();
    root.put("ID", UUID.randomUUID().toString());
    root.put("avatar_content", null);
    root.put("avatar_contentId", "avatar-doc-1");
    root.put("avatar_status", StatusCode.CLEAN);

    cut.processAfter(readEventContext, List.of(root));

    assertThat(root.get("avatar_content")).isInstanceOf(LazyProxyInputStream.class);
  }

  @Test
  void processAfterInlineAttachmentWithStaleScanTriggersRescan() {
    mockEventContext(InlineOnly_.CDS_NAME, mock(CqnSelect.class));

    var root = CdsData.create();
    // Null key so areKeysEmpty returns true → verifyStatus proceeds
    root.put("ID", null);
    root.put("avatar_content", null);
    root.put("avatar_contentId", "avatar-doc-stale");
    root.put("avatar_status", StatusCode.CLEAN);
    // No scannedAt → stale → triggers transitionToScanning with inline prefix

    cut.processAfter(readEventContext, List.of(root));

    // transitionToScanning calls persistenceService.run(update) with prefixed columns
    verify(persistenceService).run(any(com.sap.cds.ql.cqn.CqnUpdate.class));
    verify(asyncMalwareScanExecutor)
        .scanAsync(any(), eq("avatar-doc-stale"), eq(Optional.of("avatar")));
    assertThat(root.get("avatar_content")).isInstanceOf(LazyProxyInputStream.class);
  }
}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class CreateAttachmentEventTest {

  private static final String TEST_FULL_NAME = "test.full.Name";

  private CreateAttachmentEvent cut;

  private AttachmentService attachmentService;
  private ListenerProvider listenerProvider;
  private Path path;
  private ResolvedSegment target;
  private CdsEntity entity;
  private ArgumentCaptor<CreateAttachmentInput> contextArgumentCaptor;
  private EventContext eventContext;
  private ChangeSetContext changeSetContext;
  private ParameterInfo parameterInfo;

  @BeforeEach
  void setup() {
    attachmentService = mock(AttachmentService.class);
    listenerProvider = mock(ListenerProvider.class);
    cut = new CreateAttachmentEvent(attachmentService, listenerProvider);

    contextArgumentCaptor = ArgumentCaptor.forClass(CreateAttachmentInput.class);
    path = mock(Path.class);
    target = mock(ResolvedSegment.class);
    entity = mock(CdsEntity.class);
    eventContext = mock(EventContext.class);
    changeSetContext = mock(ChangeSetContext.class);
    parameterInfo = mock(ParameterInfo.class);
    when(eventContext.getChangeSetContext()).thenReturn(changeSetContext);
    when(eventContext.getParameterInfo()).thenReturn(parameterInfo);
    when(target.entity()).thenReturn(entity);
    when(path.target()).thenReturn(target);
  }

  @Test
  void storageCalledWithAllFieldsFilledFromPath() {
    when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
    var attachment = prepareAndExecuteEventWithData();

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    var resultValue = contextArgumentCaptor.getValue();
    assertThat(resultValue.attachmentIds()).containsEntry("ID", attachment.getId());
    assertThat(resultValue.attachmentEntity()).isEqualTo(entity);
    assertThat(resultValue.mimeType()).isEqualTo(attachment.getMimeType());
    assertThat(resultValue.fileName()).isEqualTo(attachment.getFileName());
    assertThat(resultValue.content()).isEqualTo(attachment.getContent());
  }

  @Test
  void storageCalledWithAllFieldsFilledFromExistingData() {
    when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);
    var attachment = Attachments.create();

    attachment.setContent(mock(InputStream.class));
    attachment.setId(UUID.randomUUID().toString());
    attachment.put("up__ID", "test");

    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId(), "up__ID", "test"));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    var existingData = Attachments.create();
    existingData.setFileName("some file name");
    existingData.setMimeType("some mime type");

    cut.processEvent(path, attachment.getContent(), existingData, eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    var createInput = contextArgumentCaptor.getValue();
    assertThat(createInput.attachmentIds())
        .hasSize(2)
        .containsEntry("ID", attachment.getId())
        .containsEntry("up__ID", "test");
    assertThat(createInput.attachmentEntity()).isEqualTo(entity);
    assertThat(createInput.mimeType()).isEqualTo(existingData.get(MediaData.MIME_TYPE));
    assertThat(createInput.fileName()).isEqualTo(existingData.get(MediaData.FILE_NAME));
    assertThat(createInput.content()).isEqualTo(attachment.getContent());
  }

  @Test
  void resultFromServiceStoredInPath() {
    var attachment = Attachments.create();
    attachment.setId("test");
    var attachmentServiceResult =
        new AttachmentModificationResult(false, "some document id", "test", null);
    when(attachmentService.createAttachment(any())).thenReturn(attachmentServiceResult);
    when(target.values()).thenReturn(attachment);

    cut.processEvent(path, attachment.getContent(), Attachments.create(), eventContext);

    assertThat(attachment.getContentId()).isEqualTo(attachmentServiceResult.contentId());
    assertThat(attachment.getStatus()).isEqualTo(attachmentServiceResult.status());
  }

  @Test
  void changesetIstRegistered() {
    var contentId = "document id";
    var runtime = mock(CdsRuntime.class);
    when(eventContext.getCdsRuntime()).thenReturn(runtime);
    var listener = mock(ChangeSetListener.class);
    when(listenerProvider.provideListener(contentId, runtime)).thenReturn(listener);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, contentId, "test", null));

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(changeSetContext).register(listener);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void contentIsReturnedIfNotExternalStored(boolean isExternalStored) throws IOException {
    var attachment = Attachments.create();

    var testContent = "test content";
    try (var testContentStream =
        new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8))) {
      attachment.setContent(testContentStream);
      attachment.setId(UUID.randomUUID().toString());
    }
    when(target.values()).thenReturn(attachment);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(isExternalStored, "id", "test", null));

    var result =
        cut.processEvent(path, attachment.getContent(), Attachments.create(), eventContext);

    var expectedContent = isExternalStored ? attachment.getContent() : null;
    assertThat(result).isEqualTo(expectedContent);
  }

  private Attachments prepareAndExecuteEventWithData() {
    var attachment = Attachments.create();

    attachment.setContent(mock(InputStream.class));
    attachment.setMimeType("mimeType");
    attachment.setFileName("file name");
    attachment.setId(UUID.randomUUID().toString());

    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));

    cut.processEvent(path, attachment.getContent(), Attachments.create(), eventContext);
    return attachment;
  }

  @Test
  void fileNameFromRfc5987Header() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename*=UTF-8''my%20file%20name.pdf");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("my file name.pdf");
    assertThat(attachment.getFileName()).isEqualTo("my file name.pdf");
  }

  @Test
  void fileNameFromRfc5987HeaderWithTrailingParams() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename*=UTF-8''my%20file.pdf; size=1234");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("my file.pdf");
    assertThat(attachment.getFileName()).isEqualTo("my file.pdf");
  }

  @Test
  void fileNameFromPlainHeader() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"report.pdf\"");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("report.pdf");
  }

  @Test
  void fileNameFromPlainHeaderWithoutQuotes() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=report.pdf");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("report.pdf");
  }

  @Test
  void fileNameFromSlugHeader() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition")).thenReturn(null);
    when(parameterInfo.getHeader("slug")).thenReturn("document.docx");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("document.docx");
  }

  @Test
  void fileNamePayloadPrecedesHeader() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    attachment.setFileName("payload-name.pdf");
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"header-name.pdf\"");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("payload-name.pdf");
  }

  @Test
  void mimeTypeFromContentTypeHeader() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Type")).thenReturn("image/jpeg; charset=utf-8");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isEqualTo("image/jpeg");
    assertThat(attachment.getMimeType()).isEqualTo("image/jpeg");
  }

  @Test
  void mimeTypePayloadPrecedesHeader() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    attachment.setMimeType("text/plain");
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Type")).thenReturn("application/pdf");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isEqualTo("text/plain");
  }

  @Test
  void fileNameIgnoredForInvalidHeader() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Disposition")).thenReturn("inline");
    when(parameterInfo.getHeader("slug")).thenReturn(null);

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isNull();
  }

  @Test
  void mimeTypeFromHeaderWhenEmpty() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(parameterInfo.getHeader("Content-Type")).thenReturn("text/csv");

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isEqualTo("text/csv");
  }

  @Test
  void headersSkippedWhenParameterInfoIsNull() {
    var attachment = Attachments.create();
    attachment.setId(UUID.randomUUID().toString());
    when(target.values()).thenReturn(attachment);
    when(target.keys()).thenReturn(Map.of("ID", attachment.getId()));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "test", null));
    when(eventContext.getParameterInfo()).thenReturn(null);

    cut.processEvent(path, null, Attachments.create(), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isNull();
    assertThat(contextArgumentCaptor.getValue().mimeType()).isNull();
  }

  // --- Inline Attachment Tests ---

  @Test
  void inlineContentIdAndStatusWrittenWithPrefix() {
    CdsEntity realEntity =
        RuntimeHelper.runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    when(target.entity()).thenReturn(realEntity);

    Map<String, Object> values = new HashMap<>();
    values.put("ID", UUID.randomUUID().toString());
    values.put("profilePicture_mimeType", "image/png");
    values.put("profilePicture_fileName", "photo.png");
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));

    var content = mock(InputStream.class);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "doc-123", "Clean", null));

    cut.processEvent(path, content, inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_contentId", "doc-123");
    assertThat(values).containsEntry("profilePicture_status", "Clean");
  }

  @Test
  void inlinePrefixedFieldValuesPassedToService() {
    CdsEntity realEntity =
        RuntimeHelper.runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    when(target.entity()).thenReturn(realEntity);

    Map<String, Object> values = new HashMap<>();
    values.put("ID", UUID.randomUUID().toString());
    values.put("profilePicture_mimeType", "image/jpeg");
    values.put("profilePicture_fileName", "avatar.jpg");
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));

    var content = mock(InputStream.class);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "ok", null));

    cut.processEvent(path, content, inlineAttachment("profilePicture"), eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    var input = contextArgumentCaptor.getValue();
    assertThat(input.mimeType()).isEqualTo("image/jpeg");
    assertThat(input.fileName()).isEqualTo("avatar.jpg");
    assertThat(input.content()).isEqualTo(content);
  }

  @Test
  void inlineFallsBackToAttachmentObjectWhenPrefixedFieldMissing() {
    CdsEntity realEntity =
        RuntimeHelper.runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    when(target.entity()).thenReturn(realEntity);

    Map<String, Object> values = new HashMap<>();
    values.put("ID", UUID.randomUUID().toString());
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));

    var content = mock(InputStream.class);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "ok", null));

    var existingData = Attachments.create();
    existingData.setFileName("fallback.txt");
    existingData.setMimeType("text/plain");
    existingData.put(ApplicationHandlerHelper.INLINE_PREFIX_MARKER, "profilePicture");

    cut.processEvent(path, content, existingData, eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    var input = contextArgumentCaptor.getValue();
    assertThat(input.mimeType()).isEqualTo("text/plain");
    assertThat(input.fileName()).isEqualTo("fallback.txt");
  }

  @Test
  void nonInlineEntityDoesNotUsePrefixedFields() {
    when(entity.getQualifiedName()).thenReturn(TEST_FULL_NAME);

    Map<String, Object> values = new HashMap<>();
    values.put("ID", UUID.randomUUID().toString());
    values.put(MediaData.MIME_TYPE, "application/pdf");
    values.put(MediaData.FILE_NAME, "doc.pdf");
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));

    var content = mock(InputStream.class);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "doc-999", "ok", null));

    cut.processEvent(path, content, Attachments.create(), eventContext);

    assertThat(values).containsEntry(Attachments.CONTENT_ID, "doc-999");
    assertThat(values).containsEntry(Attachments.STATUS, "ok");
  }

  @Test
  void processEventWritesScannedAtWhenNonNull() {
    CdsEntity realEntity =
        RuntimeHelper.runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    when(target.entity()).thenReturn(realEntity);

    Map<String, Object> values = new HashMap<>();
    values.put("ID", UUID.randomUUID().toString());
    values.put("profilePicture_mimeType", "image/png");
    values.put("profilePicture_fileName", "photo.png");
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));

    var scannedAt = java.time.Instant.now();
    var content = mock(InputStream.class);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "doc-scan", "Clean", scannedAt));

    cut.processEvent(path, content, inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_contentId", "doc-scan");
    assertThat(values).containsEntry("profilePicture_status", "Clean");
    assertThat(values).containsEntry("profilePicture_scannedAt", scannedAt);
  }

  // --- Inline Header Extraction Tests ---

  private Map<String, Object> prepareInlineValuesWithoutMetadata() {
    CdsEntity realEntity =
        RuntimeHelper.runtime.getCdsModel().findEntity(RootTable_.CDS_NAME).orElseThrow();
    when(target.entity()).thenReturn(realEntity);

    Map<String, Object> values = new HashMap<>();
    values.put("ID", UUID.randomUUID().toString());
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "ok", null));
    return values;
  }

  @Test
  void inlineExtractsFileNameFromRfc5987Header() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename*=UTF-8''my%20file.txt");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_fileName", "my file.txt");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("my file.txt");
  }

  @Test
  void inlineExtractsFileNameFromPlainHeader() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Disposition"))
        .thenReturn("attachment; filename=\"report.pdf\"");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_fileName", "report.pdf");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("report.pdf");
  }

  @Test
  void inlineExtractsFileNameFromSlugHeader() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Disposition")).thenReturn(null);
    when(parameterInfo.getHeader("slug")).thenReturn("slug-file.png");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_fileName", "slug-file.png");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isEqualTo("slug-file.png");
  }

  @Test
  void inlineBothHeadersNullReturnsEmptyFileName() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Disposition")).thenReturn(null);
    when(parameterInfo.getHeader("slug")).thenReturn(null);

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).doesNotContainKey("profilePicture_fileName");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().fileName()).isNull();
  }

  @Test
  void inlineExtractsMimeTypeFromContentTypeHeader() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Type")).thenReturn("image/jpeg; charset=utf-8");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_mimeType", "image/jpeg");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isEqualTo("image/jpeg");
  }

  @Test
  void inlineMimeTypeOctetStreamKeptWhenExplicitlySet() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    values.put("profilePicture_mimeType", "application/octet-stream");
    when(parameterInfo.getHeader("Content-Type")).thenReturn("image/png");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_mimeType", "application/octet-stream");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isEqualTo("application/octet-stream");
  }

  @Test
  void inlineMimeTypeNullContentTypeReturnsEmpty() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Type")).thenReturn(null);

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).doesNotContainKey("profilePicture_mimeType");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isNull();
  }

  @Test
  void inlineMimeTypeOctetStreamFromContentTypeHeaderIsUsed() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    when(parameterInfo.getHeader("Content-Type")).thenReturn("application/octet-stream");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    assertThat(values).containsEntry("profilePicture_mimeType", "application/octet-stream");
    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    assertThat(contextArgumentCaptor.getValue().mimeType()).isEqualTo("application/octet-stream");
  }

  @Test
  void inlineFileNameAlreadyPresentSkipsHeaderExtraction() {
    Map<String, Object> values = prepareInlineValuesWithoutMetadata();
    values.put("profilePicture_fileName", "already-set.pdf");

    cut.processEvent(
        path, mock(InputStream.class), inlineAttachment("profilePicture"), eventContext);

    verify(parameterInfo, never()).getHeader("Content-Disposition");
    assertThat(values).containsEntry("profilePicture_fileName", "already-set.pdf");
  }

  private static Attachments inlineAttachment(String prefix) {
    Attachments attachment = Attachments.create();
    attachment.put(ApplicationHandlerHelper.INLINE_PREFIX_MARKER, prefix);
    return attachment;
  }
}

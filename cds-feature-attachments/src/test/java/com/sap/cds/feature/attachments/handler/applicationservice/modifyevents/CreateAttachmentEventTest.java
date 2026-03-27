/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
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
    when(eventContext.getChangeSetContext()).thenReturn(changeSetContext);
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

  // --- Inline Attachment Tests ---

  @Test
  void inlineContentIdAndStatusWrittenWithPrefix() {
    // Use real entity from CDS model so that getInlineAttachmentFieldNames returns
    // ["profilePicture"]
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

    cut.processEvent(path, content, Attachments.create(), eventContext);

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

    cut.processEvent(path, content, Attachments.create(), eventContext);

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
    // No prefixed mimeType/fileName in values
    when(target.values()).thenReturn(values);
    when(target.keys()).thenReturn(Map.of("ID", values.get("ID")));

    var content = mock(InputStream.class);
    when(attachmentService.createAttachment(any()))
        .thenReturn(new AttachmentModificationResult(false, "id", "ok", null));

    var existingData = Attachments.create();
    existingData.setFileName("fallback.txt");
    existingData.setMimeType("text/plain");

    cut.processEvent(path, content, existingData, eventContext);

    verify(attachmentService).createAttachment(contextArgumentCaptor.capture());
    var input = contextArgumentCaptor.getValue();
    assertThat(input.mimeType()).isEqualTo("text/plain");
    assertThat(input.fileName()).isEqualTo("fallback.txt");
  }

  @Test
  void nonInlineEntityDoesNotUsePrefixedFields() {
    // Mock entity that is NOT inline
    // Plain mock has no elements, so getInlineAttachmentFieldNames returns empty
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

    // Fields written with unprefixed names
    assertThat(values).containsEntry(Attachments.CONTENT_ID, "doc-999");
    assertThat(values).containsEntry(Attachments.STATUS, "ok");
  }
}

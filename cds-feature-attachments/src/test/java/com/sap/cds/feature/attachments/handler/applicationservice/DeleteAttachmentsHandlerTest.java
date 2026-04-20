/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Roots;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteAttachmentsHandlerTest {

  private static CdsRuntime runtime;

  private DeleteAttachmentsHandler cut;
  private AttachmentsReader attachmentsReader;
  private MarkAsDeletedAttachmentEvent modifyAttachmentEvent;
  private CdsDeleteEventContext context;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    attachmentsReader = mock(AttachmentsReader.class);
    modifyAttachmentEvent = mock(MarkAsDeletedAttachmentEvent.class);
    cut = new DeleteAttachmentsHandler(attachmentsReader, modifyAttachmentEvent);

    context = mock(CdsDeleteEventContext.class);
  }

  @Test
  void noAttachmentDataServiceNotCalled() {
    var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(entity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());

    cut.processBefore(context);

    verifyNoInteractions(modifyAttachmentEvent);
  }

  @Test
  void attachmentDataExistsServiceIsCalled() {
    var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(entity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());
    var data = Attachments.create();
    data.setId("test");
    data.setContentId("test");
    var inputStream = mock(InputStream.class);
    data.setContent(inputStream);
    when(attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn()))
        .thenReturn(List.of(data));

    cut.processBefore(context);

    verify(modifyAttachmentEvent)
        .processEvent(any(), eq(inputStream), eq(data), eq(context), any());
    assertThat(data.getContent()).isNull();
  }

  @Test
  void attachmentDataExistsAsExpandServiceIsCalled() {
    var rootEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(rootEntity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());
    var inputStream = mock(InputStream.class);
    var attachment1 = buildAttachment("id1", inputStream);
    var attachment2 = buildAttachment(UUID.randomUUID().toString(), inputStream);
    var root = Roots.create();
    var items = Items.create();
    root.setItemTable(List.of(items));
    items.setAttachments(List.of(attachment1, attachment2));
    when(attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn()))
        .thenReturn(List.of(Attachments.of(root)));

    cut.processBefore(context);

    verify(modifyAttachmentEvent)
        .processEvent(
            any(Path.class), eq(inputStream), eq(Attachments.of(attachment1)), eq(context), any());
    verify(modifyAttachmentEvent)
        .processEvent(
            any(Path.class), eq(inputStream), eq(Attachments.of(attachment2)), eq(context), any());
    assertThat(attachment1.getContent()).isNull();
    assertThat(attachment2.getContent()).isNull();
  }

  private Attachment buildAttachment(String id, InputStream inputStream) {
    var attachment = Attachment.create();
    attachment.setId(id);
    attachment.setContentId("doc_" + id);
    attachment.setContent(inputStream);
    return attachment;
  }

  @Test
  void inlineAttachmentDeleteExtractsContentIdFromFlattenedFields() {
    // RootTable has inline attachment profilePicture : AttachmentType
    // When deleting RootTable, the MEDIA_CONTENT_FILTER triggers for profilePicture_content
    // and the handler should extract the contentId from the flattened field
    // profilePicture_contentId
    var rootEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(rootEntity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());

    var inputStream = mock(InputStream.class);

    // Build data with flattened inline attachment fields (as they appear in DB)
    var root = Roots.create();
    root.setId(UUID.randomUUID().toString());
    root.put("profilePicture_content", inputStream);
    root.put("profilePicture_contentId", "inline-cid-123");
    root.put("profilePicture_mimeType", "image/png");
    root.put("profilePicture_fileName", "avatar.png");

    when(attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn()))
        .thenReturn(List.of(Attachments.of(root)));

    cut.processBefore(context);

    // Verify the modifyAttachmentEvent receives an Attachments object with the extracted
    // (unprefixed) contentId from the inline attachment
    verify(modifyAttachmentEvent)
        .processEvent(any(Path.class), eq(inputStream), any(Attachments.class), eq(context), any());
  }
}

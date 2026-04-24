/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.CountingInputStream;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DraftPatchAttachmentsHandlerTest {

  private static CdsRuntime runtime;

  private DraftPatchAttachmentsHandler cut;
  private DraftPatchEventContext eventContext;
  private PersistenceService persistence;
  private ModifyAttachmentEventFactory eventFactory;
  private ModifyAttachmentEvent event;
  private ArgumentCaptor<CqnSelect> selectCaptor;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    persistence = mock(PersistenceService.class);
    eventFactory = mock(ModifyAttachmentEventFactory.class);
    cut =
        new DraftPatchAttachmentsHandler(
            persistence, eventFactory, ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER);
    eventContext = mock(DraftPatchEventContext.class);
    event = mock(ModifyAttachmentEvent.class);
    when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
    selectCaptor = ArgumentCaptor.forClass(CqnSelect.class);
    ParameterInfo parameterInfo = mock(ParameterInfo.class);
    when(eventContext.getParameterInfo()).thenReturn(parameterInfo);
  }

  @Test
  void draftEntityReadAndUsed() {
    getEntityAndMockContext(RootTable_.CDS_NAME);
    var root = buildRooWithAttachment(Attachments.create());
    when(persistence.run(any(CqnSelect.class))).thenReturn(mock(Result.class));

    cut.processBeforeDraftPatch(eventContext, List.of(Attachments.of(root)));

    verify(persistence).run(selectCaptor.capture());
    var select = selectCaptor.getValue();
    assertThat(select.from().toString())
        .contains(Attachment_.CDS_NAME + DraftUtils.DRAFT_TABLE_POSTFIX);
    assertThat(select.getLock()).isEmpty();
  }

  @Test
  void draftEntityUsed() {
    var draftAttachmentName = Attachment_.CDS_NAME + DraftUtils.DRAFT_TABLE_POSTFIX;
    getEntityAndMockContext(draftAttachmentName);
    var attachment = Attachments.create();
    attachment.setContent(mock(InputStream.class));
    when(persistence.run(any(CqnSelect.class))).thenReturn(mock(Result.class));

    cut.processBeforeDraftPatch(eventContext, List.of(attachment));

    verify(persistence).run(selectCaptor.capture());
    var select = selectCaptor.getValue();
    assertThat(select.from().toString()).contains(draftAttachmentName);
  }

  @Test
  void selectedDataUsedForEventFactory() {
    getEntityAndMockContext(RootTable_.CDS_NAME);
    var attachment = Attachments.create();
    var root = buildRooWithAttachment(attachment);
    var content = attachment.getContent();
    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);
    when(result.listOf(Attachments.class)).thenReturn(List.of(attachment));

    cut.processBeforeDraftPatch(eventContext, List.of(Attachments.of(root)));

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(eventFactory)
        .getEvent(streamCaptor.capture(), eq(attachment.getContentId()), eq(attachment));
    InputStream captured = streamCaptor.getValue();
    assertThat(captured).isInstanceOf(CountingInputStream.class);
    assertThat(((CountingInputStream) captured).getDelegate()).isSameAs(content);
  }

  @Test
  void contentIdUsedForEventFactory() {
    getEntityAndMockContext(RootTable_.CDS_NAME);
    var attachment = Attachments.create();
    var root = buildRooWithAttachment(attachment);
    attachment.setContentId(UUID.randomUUID().toString());
    var content = attachment.getContent();
    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);
    when(result.listOf(Attachments.class)).thenReturn(List.of(attachment));

    cut.processBeforeDraftPatch(eventContext, List.of(Attachments.of(root)));

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    verify(eventFactory)
        .getEvent(streamCaptor.capture(), eq(attachment.getContentId()), eq(attachment));
    InputStream captured = streamCaptor.getValue();
    assertThat(captured).isInstanceOf(CountingInputStream.class);
    assertThat(((CountingInputStream) captured).getDelegate()).isSameAs(content);
    verify(event).processEvent(any(), eq(captured), eq(attachment), eq(eventContext), any());
  }

  @Test
  void contentIdIsNotSetForNonMediaEntity() {
    getEntityAndMockContext(Events_.CDS_NAME);
    var events = Events.create();
    events.setContent("test");

    cut.processBeforeDraftPatch(eventContext, List.of(Attachments.of(events)));

    assertThat(events).doesNotContainKey(Attachments.CONTENT_ID);
  }

  @Test
  void classHasCorrectAnnotations() {
    var serviceAnnotation = cut.getClass().getAnnotation(ServiceName.class);

    assertThat(serviceAnnotation.value()).containsOnly("*");
    assertThat(serviceAnnotation.type()).containsOnly(DraftService.class);
  }

  @Test
  void methodHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("processBeforeDraftPatch", DraftPatchEventContext.class, List.class);
    var beforeAnnotation = method.getAnnotation(Before.class);
    var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(beforeAnnotation.event()).isEmpty();
    assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
  }

  private RootTable buildRooWithAttachment(Attachments attachments) {
    var items = Items.create();
    attachments.setContent(mock(InputStream.class));
    attachments.setId(UUID.randomUUID().toString());
    items.setAttachments(List.of(attachments));
    var root = RootTable.create();
    root.setItemTable(List.of(items));
    return root;
  }

  private void getEntityAndMockContext(String cdsName) {
    var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
    mockTargetInUpdateContext(serviceEntity.orElseThrow());
  }

  private void mockTargetInUpdateContext(CdsEntity serviceEntity) {
    when(eventContext.getTarget()).thenReturn(serviceEntity);
  }

  // --- Inline Attachment Tests ---

  @Test
  void inlineContentFieldTriggersConverterViaMEDIA_CONTENT_FILTER() {
    // RootTable has profilePicture : Attachment (inline).
    // MEDIA_CONTENT_FILTER should match profilePicture_content and the converter
    // should call persistence + eventFactory.
    getEntityAndMockContext(RootTable_.CDS_NAME);

    var data = CdsData.create();
    data.put("ID", UUID.randomUUID().toString());
    data.put("profilePicture_content", mock(InputStream.class));

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    // The converter reads from persistence (draft entity) and calls eventFactory
    verify(persistence).run(any(CqnSelect.class));
    verify(eventFactory).getEvent(any(), any(), any());
  }

  @Test
  void inlineDeleteExtractsExistingContentIdFromFlattenedDbResult() {
    // When the user deletes an inline attachment, the PATCH data has
    // profilePicture_content: null. The DB result has flattened column names
    // (profilePicture_contentId). The handler must extract the existing contentId
    // from the flattened DB result so the event factory can return deleteEvent.
    getEntityAndMockContext(RootTable_.CDS_NAME);

    String bookId = UUID.randomUUID().toString();
    String existingContentId = UUID.randomUUID().toString();

    // Incoming data: user deleting the inline attachment (content = null)
    var data = CdsData.create();
    data.put("ID", bookId);
    data.put("profilePicture_content", null);

    // DB result: existing draft row with flattened inline attachment fields
    var dbRow = Attachments.create();
    dbRow.put("ID", bookId);
    dbRow.put("profilePicture_contentId", existingContentId);
    dbRow.put("profilePicture_status", "Clean");
    dbRow.put("profilePicture_mimeType", "image/png");
    dbRow.put("profilePicture_fileName", "avatar.png");

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);
    when(result.listOf(Attachments.class)).thenReturn(List.of(dbRow));

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    // Verify the event factory receives an Attachments with the correctly extracted
    // (unprefixed) contentId from the DB data
    ArgumentCaptor<Attachments> attachmentCaptor = ArgumentCaptor.forClass(Attachments.class);
    verify(eventFactory).getEvent(any(), any(), attachmentCaptor.capture());
    Attachments captured = attachmentCaptor.getValue();
    assertThat(captured.getContentId()).isEqualTo(existingContentId);
  }

  // --- persistInlineAttachmentMetadata Tests ---

  @Test
  void inlinePatchPersistsMetadataWhenContentIdMimeTypeAndFileNamePresent() {
    getEntityAndMockContext(RootTable_.CDS_NAME);

    var data = CdsData.create();
    data.put("ID", UUID.randomUUID().toString());
    data.put("profilePicture_content", mock(InputStream.class));

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    // The event.processEvent simulates CreateAttachmentEvent putting metadata into data
    when(event.processEvent(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              data.put("profilePicture_contentId", "cid-123");
              data.put("profilePicture_mimeType", "image/png");
              data.put("profilePicture_fileName", "photo.png");
              return null;
            });

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    ArgumentCaptor<CqnUpdate> updateCaptor = ArgumentCaptor.forClass(CqnUpdate.class);
    verify(persistence).run(updateCaptor.capture());
    CqnUpdate update = updateCaptor.getValue();
    assertThat(update.entries()).isNotEmpty();
    assertThat(update.entries().get(0)).containsEntry("profilePicture_mimeType", "image/png");
    assertThat(update.entries().get(0)).containsEntry("profilePicture_fileName", "photo.png");
  }

  @Test
  void inlinePatchSkipsWhenContentIdNull() {
    getEntityAndMockContext(RootTable_.CDS_NAME);

    var data = CdsData.create();
    data.put("ID", UUID.randomUUID().toString());
    data.put("profilePicture_content", mock(InputStream.class));

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    // processEvent does NOT put profilePicture_contentId → contentId remains null
    when(event.processEvent(any(), any(), any(), any(), any())).thenReturn(null);

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    verify(persistence, never()).run(any(CqnUpdate.class));
  }

  @Test
  void inlinePatchSkipsUpdateWhenNoMetadata() {
    getEntityAndMockContext(RootTable_.CDS_NAME);

    var data = CdsData.create();
    data.put("ID", UUID.randomUUID().toString());
    data.put("profilePicture_content", mock(InputStream.class));

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    // processEvent puts contentId but no mimeType/fileName
    when(event.processEvent(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              data.put("profilePicture_contentId", "cid-456");
              return null;
            });

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    verify(persistence, never()).run(any(CqnUpdate.class));
  }

  @Test
  void inlinePatchPersistsOnlyMimeType() {
    getEntityAndMockContext(RootTable_.CDS_NAME);

    var data = CdsData.create();
    data.put("ID", UUID.randomUUID().toString());
    data.put("profilePicture_content", mock(InputStream.class));

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    when(event.processEvent(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              data.put("profilePicture_contentId", "cid-789");
              data.put("profilePicture_mimeType", "text/plain");
              return null;
            });

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    ArgumentCaptor<CqnUpdate> updateCaptor = ArgumentCaptor.forClass(CqnUpdate.class);
    verify(persistence).run(updateCaptor.capture());
    CqnUpdate update = updateCaptor.getValue();
    assertThat(update.entries().get(0)).containsEntry("profilePicture_mimeType", "text/plain");
    assertThat(update.entries().get(0)).doesNotContainKey("profilePicture_fileName");
  }

  @Test
  void inlinePatchPersistsOnlyFileName() {
    getEntityAndMockContext(RootTable_.CDS_NAME);

    var data = CdsData.create();
    data.put("ID", UUID.randomUUID().toString());
    data.put("profilePicture_content", mock(InputStream.class));

    var result = mock(Result.class);
    when(persistence.run(any(CqnSelect.class))).thenReturn(result);

    when(event.processEvent(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              data.put("profilePicture_contentId", "cid-000");
              data.put("profilePicture_fileName", "document.pdf");
              return null;
            });

    cut.processBeforeDraftPatch(eventContext, List.of(data));

    ArgumentCaptor<CqnUpdate> updateCaptor = ArgumentCaptor.forClass(CqnUpdate.class);
    verify(persistence).run(updateCaptor.capture());
    CqnUpdate update = updateCaptor.getValue();
    assertThat(update.entries().get(0)).containsEntry("profilePicture_fileName", "document.pdf");
    assertThat(update.entries().get(0)).doesNotContainKey("profilePicture_mimeType");
  }

}

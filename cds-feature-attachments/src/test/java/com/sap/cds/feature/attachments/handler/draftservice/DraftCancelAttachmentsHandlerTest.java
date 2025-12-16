/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DraftCancelAttachmentsHandlerTest {

  private static CdsRuntime runtime;

  private DraftCancelAttachmentsHandler cut;
  private AttachmentsReader attachmentsReader;
  private MarkAsDeletedAttachmentEvent deleteContentAttachmentEvent;
  private DraftCancelEventContext eventContext;
  private ArgumentCaptor<CqnDelete> deleteArgumentCaptor;
  private ArgumentCaptor<Attachments> dataArgumentCaptor;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    attachmentsReader = mock(AttachmentsReader.class);
    deleteContentAttachmentEvent = mock(MarkAsDeletedAttachmentEvent.class);
    cut = new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentAttachmentEvent);

    eventContext = mock(DraftCancelEventContext.class);
    deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
    dataArgumentCaptor = ArgumentCaptor.forClass(Attachments.class);
  }

  @Test
  void entityHasNoAttachmentsAndIsNotAttachmentEntityNothingHappens() {
    // Test the case where isAttachmentEntity and hasAttachmentAssociations both
    // return false
    CdsEntity mockEntity = mock(CdsEntity.class);
    // Entity has no elements with name "attachment"
    when(mockEntity.getQualifiedName())
        .thenReturn("TestService.RegularEntity"); // No "Attachment" in name
    when(mockEntity.getAnnotationValue("_is_media_data", false)).thenReturn(false);
    when(mockEntity.elements()).thenReturn(java.util.stream.Stream.empty());

    com.sap.cds.reflect.CdsModel mockModel = mock(com.sap.cds.reflect.CdsModel.class);
    when(mockModel.getEntity("TestService.RegularEntity")).thenReturn(mockEntity);

    when(eventContext.getTarget()).thenReturn(mockEntity);
    when(eventContext.getModel()).thenReturn(mockModel);

    CqnDelete mockDelete = mock(CqnDelete.class);
    when(mockDelete.where()).thenReturn(Optional.empty());
    when(eventContext.getCqn()).thenReturn(mockDelete);
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");

    cut.processBeforeDraftCancel(eventContext);

    verifyNoInteractions(attachmentsReader);
  }

  @Test
  void entityWithoutAttachmentsIsSkipped() {
    // Test that entities without attachments are properly skipped
    CdsEntity mockEntity = mock(CdsEntity.class);
    when(mockEntity.getQualifiedName()).thenReturn("TestService.RegularEntity");
    when(mockEntity.getAnnotationValue("_is_media_data", false)).thenReturn(false);
    when(mockEntity.compositions()).thenReturn(java.util.stream.Stream.empty());

    when(eventContext.getTarget()).thenReturn(mockEntity);
    when(eventContext.getCqn()).thenReturn(Delete.from("RegularEntity"));
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

    cut.processBeforeDraftCancel(eventContext);

    verifyNoInteractions(attachmentsReader, deleteContentAttachmentEvent);
  }

  @Test
  void nothingSelectedNothingToDo() {
    getEntityAndMockContext(RootTable_.CDS_NAME);
    CqnDelete delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");

    cut.processBeforeDraftCancel(eventContext);

    verifyNoInteractions(deleteContentAttachmentEvent);
  }

  @Test
  void attachmentReaderCorrectCalled() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    CqnDelete delete = Delete.from(Attachment_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");

    cut.processBeforeDraftCancel(eventContext);

    CdsEntity target = eventContext.getTarget();
    verify(attachmentsReader)
        .readAttachments(eq(runtime.getCdsModel()), eq(target), deleteArgumentCaptor.capture());
    // Check if the modified CqnDelete that is passed to readAttachments looks
    // correct
    CqnDelete modifiedCQN = deleteArgumentCaptor.getValue();
    assertThat(modifiedCQN.toJson())
        .isEqualTo(
            "{\"DELETE\":{\"from\":{\"ref\":[{\"id\":\"unit.test.TestService.Attachment\",\"where\":[{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}]}}}");

    deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
    CdsEntity siblingTarget = target.getTargetOf(Drafts.SIBLING_ENTITY);
    verify(attachmentsReader)
        .readAttachments(
            eq(runtime.getCdsModel()), eq(siblingTarget), deleteArgumentCaptor.capture());
    CqnDelete siblingDelete = deleteArgumentCaptor.getValue();
    assertThat(siblingDelete.toJson()).isNotEqualTo(delete.toJson());
  }

  @Test
  void attachmentReaderCorrectCalledForEntityWithAttachmentAssociations() {
    getEntityAndMockContext(RootTable_.CDS_NAME);
    CqnDelete delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");

    cut.processBeforeDraftCancel(eventContext);

    CdsEntity target = eventContext.getTarget();
    verify(attachmentsReader)
        .readAttachments(eq(runtime.getCdsModel()), eq(target), deleteArgumentCaptor.capture());
    // Check if the modified CqnDelete that is passed to readAttachments looks
    // correct
    CqnDelete modifiedCQN = deleteArgumentCaptor.getValue();
    assertThat(modifiedCQN.toJson())
        .isEqualTo(
            "{\"DELETE\":{\"from\":{\"ref\":[{\"id\":\"unit.test.TestService.RootTable\",\"where\":[{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}]}}}");

    deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
    CdsEntity siblingTarget = target.getTargetOf(Drafts.SIBLING_ENTITY);
    verify(attachmentsReader)
        .readAttachments(
            eq(runtime.getCdsModel()), eq(siblingTarget), deleteArgumentCaptor.capture());
    CqnDelete siblingDelete = deleteArgumentCaptor.getValue();
    assertThat(siblingDelete.toJson()).isNotEqualTo(delete.toJson());
  }

  @Test
  void modifierCalledWithCorrectEntitiesIfDraftIsInContext() {
    getEntityAndMockContext(Attachment_.CDS_NAME + DraftUtils.DRAFT_TABLE_POSTFIX);
    CqnDelete delete = Delete.from(Attachment_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");

    cut.processBeforeDraftCancel(eventContext);

    CdsEntity target = eventContext.getTarget();
    verify(attachmentsReader)
        .readAttachments(eq(runtime.getCdsModel()), eq(target), deleteArgumentCaptor.capture());
    CdsEntity siblingTarget = target.getTargetOf(Drafts.SIBLING_ENTITY);
    verify(attachmentsReader)
        .readAttachments(
            eq(runtime.getCdsModel()), eq(siblingTarget), deleteArgumentCaptor.capture());
    CqnDelete siblingDelete = deleteArgumentCaptor.getValue();
    assertThat(siblingDelete.toJson())
        .isEqualTo(
            "{\"DELETE\":{\"from\":{\"ref\":[{\"id\":\"unit.test.TestService.Attachment\",\"where\":[{\"ref\":[\"IsActiveEntity\"]},\"=\",{\"val\":true}]}]}}}");
  }

  @Test
  void createdEntityNeedsToBeDeleted() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    CqnDelete delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");
    CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);
    Attachment attachment = buildAttachmentAndReturnByReader("test", siblingTarget, false, "");

    cut.processBeforeDraftCancel(eventContext);

    verify(deleteContentAttachmentEvent)
        .processEvent(any(), eq(null), dataArgumentCaptor.capture(), eq(eventContext));
    assertThat(dataArgumentCaptor.getValue()).isEqualTo(attachment);
  }

  @Test
  void updatedEntityNeedsToBeDeleted() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    var delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");
    CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);
    var id = UUID.randomUUID().toString();
    Attachment draftAttachment = buildAttachmentAndReturnByReader("test", siblingTarget, true, id);
    buildAttachmentAndReturnByReader("test origin", eventContext.getTarget(), false, id);

    cut.processBeforeDraftCancel(eventContext);

    verify(deleteContentAttachmentEvent)
        .processEvent(any(), eq(null), dataArgumentCaptor.capture(), eq(eventContext));
    assertThat(dataArgumentCaptor.getValue()).isEqualTo(draftAttachment);
  }

  @Test
  void entityNotUpdatedNothingToDelete() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    var delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");
    CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);
    var id = UUID.randomUUID().toString();
    var contentId = UUID.randomUUID().toString();
    buildAttachmentAndReturnByReader(contentId, siblingTarget, true, id);
    buildAttachmentAndReturnByReader(contentId, eventContext.getTarget(), false, id);

    cut.processBeforeDraftCancel(eventContext);

    verifyNoInteractions(deleteContentAttachmentEvent);
  }

  @Test
  void circularReferenceInCompositionsHandled() {
    // Test that circular references in entity compositions are handled correctly
    getEntityAndMockContext(RootTable_.CDS_NAME);
    CqnDelete delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");

    // The deepSearchForAttachmentsRecursive should handle circular references via
    // the visited set
    cut.processBeforeDraftCancel(eventContext);

    // Should complete without stack overflow or infinite loop
    verify(attachmentsReader, atLeastOnce()).readAttachments(any(), any(), any());
  }

  @Test
  void noMatchingActiveEntryForDraftAttachment() {
    getEntityAndMockContext(Attachment_.CDS_NAME);
    CqnDelete delete = Delete.from(RootTable_.class);
    when(eventContext.getCqn()).thenReturn(delete);
    when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
    when(eventContext.getEvent()).thenReturn("DRAFT_CANCEL");
    CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);

    // Create draft attachment with hasActiveEntity=true but different keys
    var id = UUID.randomUUID().toString();
    Attachment draftAttachment = Attachment.create();
    draftAttachment.setId(id);
    draftAttachment.setContentId("draft-content");
    draftAttachment.setHasActiveEntity(true);
    draftAttachment.setContent(null);
    when(attachmentsReader.readAttachments(any(), eq(siblingTarget), any()))
        .thenReturn(List.of(Attachments.of(draftAttachment)));

    // Return active attachment with different ID (no key match)
    Attachment activeAttachment = Attachment.create();
    activeAttachment.setId(UUID.randomUUID().toString());
    activeAttachment.setContentId("active-content");
    activeAttachment.setHasActiveEntity(false);
    activeAttachment.setContent(null);
    CdsEntity activeTarget = eventContext.getTarget();
    when(attachmentsReader.readAttachments(any(), eq(activeTarget), any()))
        .thenReturn(List.of(Attachments.of(activeAttachment)));

    cut.processBeforeDraftCancel(eventContext);

    // Should not call deleteEvent since keys don't match
    verifyNoInteractions(deleteContentAttachmentEvent);
  }

  private Attachment buildAttachmentAndReturnByReader(
      String contentId, CdsEntity target, boolean hasActiveEntity, String id) {
    Attachment attachment = Attachment.create();
    attachment.setId(id);
    attachment.setContentId(contentId);
    attachment.setHasActiveEntity(hasActiveEntity);
    attachment.setContent(null);
    when(attachmentsReader.readAttachments(any(), eq(target), any()))
        .thenReturn(List.of(Attachments.of(attachment)));
    return attachment;
  }

  private void getEntityAndMockContext(String cdsName) {
    Optional<CdsEntity> serviceEntity = runtime.getCdsModel().findEntity(cdsName);
    when(eventContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
  }
}

package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.draftservice.modifier.ActiveEntityModifierProvider;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.Modifier;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.runtime.CdsRuntime;

class DraftCancelAttachmentsHandlerTest {

	private static CdsRuntime runtime;

	private DraftCancelAttachmentsHandler cut;
	private AttachmentsReader attachmentsReader;
	private ModifyAttachmentEvent deleteContentAttachmentEvent;
	private ActiveEntityModifierProvider modifierProvider;
	private DraftCancelEventContext eventContext;
	private ArgumentCaptor<CqnDelete> deleteArgumentCaptor;
	private ArgumentCaptor<CdsData> dataArgumentCaptor;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		attachmentsReader = mock(AttachmentsReader.class);
		deleteContentAttachmentEvent = mock(ModifyAttachmentEvent.class);
		modifierProvider = mock(ActiveEntityModifierProvider.class);
		cut = new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentAttachmentEvent, modifierProvider);

		eventContext = mock(DraftCancelEventContext.class);
		when(modifierProvider.getModifier(anyBoolean(), anyString())).thenReturn(new Modifier() {
		});
		deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
		dataArgumentCaptor = ArgumentCaptor.forClass(CdsData.class);
	}

	@Test
	void whereConditionIncludedNothingHappens() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var delete = Delete.from(RootTable_.class).where(root -> root.ID().eq("test"));
		when(eventContext.getCqn()).thenReturn(delete);

		cut.processBeforeDraftCancel(eventContext);

		verifyNoInteractions(attachmentsReader, deleteContentAttachmentEvent, modifierProvider);
	}

	@Test
	void nothingSelectedNothingToDo() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		verifyNoInteractions(deleteContentAttachmentEvent);
	}

	@Test
	void attachmentReaderCorrectCalled() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		var target = eventContext.getTarget();
		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(target), deleteArgumentCaptor.capture());
		var originDelete = deleteArgumentCaptor.getValue();
		assertThat(originDelete.toJson()).isEqualTo(delete.toJson());

		deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
		var siblingTarget = target.getTargetOf(DraftConstants.SIBLING_ENTITY);
		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq((CdsEntity) siblingTarget),
				deleteArgumentCaptor.capture());
		var siblingDelete = deleteArgumentCaptor.getValue();
		assertThat(siblingDelete.toJson()).isEqualTo(delete.toJson());
	}

	@Test
	void modifierCalledWithCorrectEntitiesIfDraftIsInContext() {
		getEntityAndMockContext(RootTable_.CDS_NAME + DraftConstants.DRAFT_TABLE_POSTFIX);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		verify(modifierProvider).getModifier(false, RootTable_.CDS_NAME + DraftConstants.DRAFT_TABLE_POSTFIX);
		verify(modifierProvider).getModifier(true, RootTable_.CDS_NAME);
	}

	@Test
	void modifierCalledWithCorrectEntitiesIfActiveEntityIsInContext() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		verify(modifierProvider).getModifier(false, RootTable_.CDS_NAME + DraftConstants.DRAFT_TABLE_POSTFIX);
		verify(modifierProvider).getModifier(true, RootTable_.CDS_NAME);
	}

	@Test
	void createdEntityNeedsToBeDeleted() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
		var siblingTarget = eventContext.getTarget().getTargetOf(DraftConstants.SIBLING_ENTITY);
		var attachment = buildAttachmentAndReturnByReader("test", siblingTarget, false, "");

		cut.processBeforeDraftCancel(eventContext);

		verify(deleteContentAttachmentEvent).processEvent(any(), eq(null), dataArgumentCaptor.capture(), eq(eventContext));
		assertThat(dataArgumentCaptor.getValue()).isEqualTo(attachment);
	}

	@Test
	void updatedEntityNeedsToBeDeleted() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
		var siblingTarget = eventContext.getTarget().getTargetOf(DraftConstants.SIBLING_ENTITY);
		var id = UUID.randomUUID().toString();
		var draftAttachment = buildAttachmentAndReturnByReader("test", siblingTarget, true, id);
		buildAttachmentAndReturnByReader("test origin", eventContext.getTarget(), false, id);

		cut.processBeforeDraftCancel(eventContext);

		verify(deleteContentAttachmentEvent).processEvent(any(), eq(null), dataArgumentCaptor.capture(), eq(eventContext));
		assertThat(dataArgumentCaptor.getValue()).isEqualTo(draftAttachment);
	}

	@Test
	void entityNotUpdatedNothingToDelete() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
		var siblingTarget = eventContext.getTarget().getTargetOf(DraftConstants.SIBLING_ENTITY);
		var id = UUID.randomUUID().toString();
		var contentId = UUID.randomUUID().toString();
		buildAttachmentAndReturnByReader(contentId, siblingTarget, true, id);
		buildAttachmentAndReturnByReader(contentId, eventContext.getTarget(), false, id);

		cut.processBeforeDraftCancel(eventContext);

		verifyNoInteractions(deleteContentAttachmentEvent);
	}

	private Attachment buildAttachmentAndReturnByReader(String contentId, CdsStructuredType target,
			boolean hasActiveEntity, String id) {
		var attachment = Attachment.create();
		attachment.setId(id);
		attachment.setContentId(contentId);
		attachment.setHasActiveEntity(hasActiveEntity);
		attachment.setContent(null);
		when(attachmentsReader.readAttachments(any(), (CdsEntity) eq(target), any())).thenReturn(List.of(attachment));
		return attachment;
	}

	private void getEntityAndMockContext(String cdsName) {
		var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		mockTargetInUpdateContext(serviceEntity.orElseThrow());
	}

	private void mockTargetInUpdateContext(CdsEntity serviceEntity) {
		when(eventContext.getTarget()).thenReturn(serviceEntity);
	}

}

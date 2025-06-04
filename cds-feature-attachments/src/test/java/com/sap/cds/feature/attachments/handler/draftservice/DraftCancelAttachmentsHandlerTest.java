package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.draft.DraftCancelEventContext;
import com.sap.cds.services.draft.Drafts;
import com.sap.cds.services.runtime.CdsRuntime;

class DraftCancelAttachmentsHandlerTest {

	private static CdsRuntime runtime;

	private DraftCancelAttachmentsHandler cut;
	private AttachmentsReader attachmentsReader;
	private ModifyAttachmentEvent deleteContentAttachmentEvent;
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
		cut = new DraftCancelAttachmentsHandler(attachmentsReader, deleteContentAttachmentEvent);

		eventContext = mock(DraftCancelEventContext.class);
		deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
		dataArgumentCaptor = ArgumentCaptor.forClass(CdsData.class);
	}

	@Test
	void whereConditionIncludedNothingHappens() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		CqnDelete delete = Delete.from(RootTable_.class).where(root -> root.ID().eq("test"));
		when(eventContext.getCqn()).thenReturn(delete);

		cut.processBeforeDraftCancel(eventContext);

		verifyNoInteractions(attachmentsReader, deleteContentAttachmentEvent);
	}

	@Test
	void nothingSelectedNothingToDo() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		CqnDelete delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		verifyNoInteractions(deleteContentAttachmentEvent);
	}

	@Test
	void attachmentReaderCorrectCalled() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		CqnDelete delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		CdsEntity target = eventContext.getTarget();
		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(target),
				deleteArgumentCaptor.capture());
		CqnDelete originDelete = deleteArgumentCaptor.getValue();
		assertThat(originDelete.toJson()).isEqualTo(delete.toJson());

		deleteArgumentCaptor = ArgumentCaptor.forClass(CqnDelete.class);
		CdsEntity siblingTarget = target.getTargetOf(Drafts.SIBLING_ENTITY);
		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(siblingTarget),
				deleteArgumentCaptor.capture());
		CqnDelete siblingDelete = deleteArgumentCaptor.getValue();
		assertThat(siblingDelete.toJson()).isNotEqualTo(delete.toJson());
	}

	@Test
	void modifierCalledWithCorrectEntitiesIfDraftIsInContext() {
		getEntityAndMockContext(RootTable_.CDS_NAME + DraftConstants.DRAFT_TABLE_POSTFIX);
		CqnDelete delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());

		cut.processBeforeDraftCancel(eventContext);

		CdsEntity target = eventContext.getTarget();
		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(target),
				deleteArgumentCaptor.capture());
		CdsEntity siblingTarget = target.getTargetOf(Drafts.SIBLING_ENTITY);
		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(siblingTarget),
				deleteArgumentCaptor.capture());
		CqnDelete siblingDelete = deleteArgumentCaptor.getValue();
		assertThat(siblingDelete.toJson()).isEqualTo(delete.toJson());
	}

	@Test
	void createdEntityNeedsToBeDeleted() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		CqnDelete delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
		CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);
		Attachment attachment = buildAttachmentAndReturnByReader("test", siblingTarget, false, "");

		cut.processBeforeDraftCancel(eventContext);

		verify(deleteContentAttachmentEvent).processEvent(any(), eq(null), dataArgumentCaptor.capture(),
				eq(eventContext));
		assertThat(dataArgumentCaptor.getValue()).isEqualTo(attachment);
	}

	@Test
	void updatedEntityNeedsToBeDeleted() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
		CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);
		var id = UUID.randomUUID().toString();
		Attachment draftAttachment = buildAttachmentAndReturnByReader("test", siblingTarget, true, id);
		buildAttachmentAndReturnByReader("test origin", eventContext.getTarget(), false, id);

		cut.processBeforeDraftCancel(eventContext);

		verify(deleteContentAttachmentEvent).processEvent(any(), eq(null), dataArgumentCaptor.capture(),
				eq(eventContext));
		assertThat(dataArgumentCaptor.getValue()).isEqualTo(draftAttachment);
	}

	@Test
	void entityNotUpdatedNothingToDelete() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var delete = Delete.from(RootTable_.class);
		when(eventContext.getCqn()).thenReturn(delete);
		when(eventContext.getModel()).thenReturn(runtime.getCdsModel());
		CdsEntity siblingTarget = eventContext.getTarget().getTargetOf(Drafts.SIBLING_ENTITY);
		var id = UUID.randomUUID().toString();
		var contentId = UUID.randomUUID().toString();
		buildAttachmentAndReturnByReader(contentId, siblingTarget, true, id);
		buildAttachmentAndReturnByReader(contentId, eventContext.getTarget(), false, id);

		cut.processBeforeDraftCancel(eventContext);

		verifyNoInteractions(deleteContentAttachmentEvent);
	}

	private Attachment buildAttachmentAndReturnByReader(String contentId, CdsEntity target, boolean hasActiveEntity,
			String id) {
		Attachment attachment = Attachment.create();
		attachment.setId(id);
		attachment.setContentId(contentId);
		attachment.setHasActiveEntity(hasActiveEntity);
		attachment.setContent(null);
		when(attachmentsReader.readAttachments(any(), eq(target), any())).thenReturn(List.of(attachment));
		return attachment;
	}

	private void getEntityAndMockContext(String cdsName) {
		Optional<CdsEntity> serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		when(eventContext.getTarget()).thenReturn(serviceEntity.orElseThrow());
	}

}

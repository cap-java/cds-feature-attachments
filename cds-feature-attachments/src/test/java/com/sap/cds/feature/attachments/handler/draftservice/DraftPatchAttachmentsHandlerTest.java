package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.draftservice.constants.DraftConstants;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;

class DraftPatchAttachmentsHandlerTest {

	private static CdsRuntime runtime;

	private DraftPatchAttachmentsHandler cut;
	private EventContext eventContext;
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
		cut = new DraftPatchAttachmentsHandler(persistence, eventFactory);
		eventContext = mock(EventContext.class);
		event = mock(ModifyAttachmentEvent.class);
		when(eventFactory.getEvent(any(), any(), anyBoolean(), any())).thenReturn(event);
		selectCaptor = ArgumentCaptor.forClass(CqnSelect.class);
	}

	@Test
	void draftEntityReadAndUsed() {
		getEntityAndMockContext(RootTable_.CDS_NAME);
		var root = buildRooWithAttachment(Attachments.create());
		when(persistence.run(any(CqnSelect.class))).thenReturn(mock(Result.class));

		cut.processBeforeDraftPatch(eventContext, List.of(root));

		verify(persistence).run(selectCaptor.capture());
		var select = selectCaptor.getValue();
		assertThat(select.from().toString()).contains(Attachment_.CDS_NAME + DraftConstants.DRAFT_TABLE_POSTFIX);
	}

	@Test
	void draftEntityUsed() {
		var draftAttachmentName = Attachment_.CDS_NAME + DraftConstants.DRAFT_TABLE_POSTFIX;
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
		when(result.listOf(CdsData.class)).thenReturn(List.of(attachment));

		cut.processBeforeDraftPatch(eventContext, List.of(root));

		verify(eventFactory).getEvent(content, attachment.getContentId(), false, attachment);
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
		when(result.listOf(CdsData.class)).thenReturn(List.of(attachment));

		cut.processBeforeDraftPatch(eventContext, List.of(root));

		verify(eventFactory).getEvent(content, attachment.getContentId(), true, attachment);
		verify(event).processEvent(any(), eq(content), eq(attachment), eq(eventContext));
	}

	@Test
	void contentIdIsNotSetForNonMediaEntity() {
		getEntityAndMockContext(Events_.CDS_NAME);
		var events = Events.create();
		events.setContent("test");

		cut.processBeforeDraftPatch(eventContext, List.of(events));

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
		var method = cut.getClass().getMethod("processBeforeDraftPatch", EventContext.class, List.class);
		var beforeAnnotation = method.getAnnotation(Before.class);
		var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(beforeAnnotation.event()).containsOnly(DraftService.EVENT_DRAFT_PATCH);
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

}

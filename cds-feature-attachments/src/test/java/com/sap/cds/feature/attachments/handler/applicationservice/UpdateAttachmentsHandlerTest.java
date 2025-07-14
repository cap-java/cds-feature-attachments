package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.DefaultModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.processor.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnFilterableStatement;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.CdsRuntime;

class UpdateAttachmentsHandlerTest {

	private static final String UP_ID = "up__ID";
	private static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";
	private static CdsRuntime runtime;

	private UpdateAttachmentsHandler cut;
	private DefaultModifyAttachmentEventFactory eventFactory;
	private AttachmentsReader attachmentsReader;
	private AttachmentService attachmentService;
	private CdsUpdateEventContext updateContext;
	private ModifyAttachmentEvent event;
	private ArgumentCaptor<CdsData> cdsDataArgumentCaptor;
	private ArgumentCaptor<CqnSelect> selectCaptor;
	private ThreadDataStorageReader storageReader;
	private UserInfo userInfo;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		eventFactory = mock(DefaultModifyAttachmentEventFactory.class);
		attachmentsReader = mock(AttachmentsReader.class);
		attachmentService = mock(AttachmentService.class);
		storageReader = mock(ThreadDataStorageReader.class);
		cut = new UpdateAttachmentsHandler(eventFactory, attachmentsReader, attachmentService, storageReader);

		event = mock(ModifyAttachmentEvent.class);
		updateContext = mock(CdsUpdateEventContext.class);
		cdsDataArgumentCaptor = ArgumentCaptor.forClass(CdsData.class);
		selectCaptor = ArgumentCaptor.forClass(CqnSelect.class);
		when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
		userInfo = mock(UserInfo.class);
	}

	@Test
	void noContentInDataNothingToDo() {
		getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();

		cut.processBefore(updateContext, List.of(attachment));

		verifyNoInteractions(eventFactory);
		verifyNoInteractions(attachmentsReader);
		verifyNoInteractions(attachmentService);
	}

	@Test
	void eventProcessorCalledForUpdate() {
		var id = getEntityAndMockContext(Attachment_.CDS_NAME);
		var testStream = mock(InputStream.class);
		var attachment = Attachments.create();
		attachment.setContent(testStream);
		attachment.setId(id);
		when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class))).thenReturn(
				List.of(attachment));

		cut.processBefore(updateContext, List.of(attachment));

		verify(eventFactory).getEvent(testStream, null, attachment);
	}

	@Test
	void readonlyFieldsAreUsedFromOwnContext() {
		getEntityAndMockContext(Attachment_.CDS_NAME);

		var readonlyUpdateFields = CdsData.create();
		readonlyUpdateFields.put(Attachment.CONTENT_ID, "Document Id");
		readonlyUpdateFields.put(Attachment.STATUS, "Status Code");
		readonlyUpdateFields.put(Attachment.SCANNED_AT, Instant.now());
		var testStream = mock(InputStream.class);
		var attachment = Attachments.create();
		attachment.setContent(testStream);
		attachment.put("DRAFT_READONLY_CONTEXT", readonlyUpdateFields);

		when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);

		cut.processBefore(updateContext, List.of(attachment));

		verify(eventFactory).getEvent(testStream, (String) readonlyUpdateFields.get(Attachment.CONTENT_ID), 
				CdsData.create());
		assertThat(attachment.get(DRAFT_READONLY_CONTEXT)).isNull();
		assertThat(attachment.getContentId()).isEqualTo(readonlyUpdateFields.get(Attachment.CONTENT_ID));
		assertThat(attachment.getStatus()).isEqualTo(readonlyUpdateFields.get(Attachment.STATUS));
		assertThat(attachment.getScannedAt()).isEqualTo(readonlyUpdateFields.get(Attachment.SCANNED_AT));
	}

	@Test
	void readonlyDataFilledForDraftActivate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);

		var updateAttachment = Attachments.create();
		updateAttachment.setContentId("Document Id");
		updateAttachment.setStatus("Status Code");
		updateAttachment.setScannedAt(Instant.now());
		updateAttachment.setContent(null);
		when(storageReader.get()).thenReturn(true);

		cut.processBeforeForDraft(updateContext, List.of(updateAttachment));

		verifyNoInteractions(eventFactory, event);
		assertThat(updateAttachment.get(DRAFT_READONLY_CONTEXT)).isNotNull();
		var readOnlyUpdateData = (CdsData) updateAttachment.get(DRAFT_READONLY_CONTEXT);
		assertThat(readOnlyUpdateData).containsEntry(Attachment.CONTENT_ID, updateAttachment.getContentId())
				.containsEntry(Attachment.STATUS, updateAttachment.getStatus())
				.containsEntry(Attachment.SCANNED_AT, updateAttachment.getScannedAt());
	}

	@Test
	void readonlyDataClearedIfNotDraftActivate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);

		var updateAttachment = Attachments.create();
		var contentId = "Document Id";
		updateAttachment.setContentId(contentId);
		updateAttachment.setContent(null);
		var readonlyData = CdsData.create();
		readonlyData.put(Attachment.STATUS, "some wrong status code");
		readonlyData.put(Attachment.CONTENT_ID, "some other document id");
		readonlyData.put(Attachment.SCANNED_AT, Instant.EPOCH);
		updateAttachment.put("DRAFT_READONLY_CONTEXT", readonlyData);
		when(storageReader.get()).thenReturn(false);

		cut.processBeforeForDraft(updateContext, List.of(updateAttachment));

		verifyNoInteractions(eventFactory, event);
		assertThat(updateAttachment.get(DRAFT_READONLY_CONTEXT)).isNull();
		assertThat(updateAttachment).containsEntry(Attachment.CONTENT_ID, contentId)
				.doesNotContainKey(Attachment.STATUS).doesNotContainKey(Attachment.SCANNED_AT);
	}

	@Test
	void readonlyDataNotFilledForNonDraftActivate() {
		getEntityAndMockContext(Attachment_.CDS_NAME);

		var updateAttachment = Attachments.create();
		updateAttachment.setContentId("Document Id");
		updateAttachment.setStatus("Status Code");
		updateAttachment.setScannedAt(Instant.now());
		when(storageReader.get()).thenReturn(false);

		cut.processBeforeForDraft(updateContext, List.of(updateAttachment));

		verifyNoInteractions(eventFactory, event);
		assertThat(updateAttachment.get(DRAFT_READONLY_CONTEXT)).isNull();
	}

	@Test
	void eventProcessorNotCalledForUpdateForDraft() {
		when(updateContext.getService()).thenReturn(mock(ApplicationService.class));
		when(updateContext.getTarget()).thenReturn(runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow());

		cut.processBeforeForDraft(updateContext, Collections.emptyList());

		verifyNoInteractions(eventFactory);
		verifyNoInteractions(attachmentsReader);
		verifyNoInteractions(attachmentService);
		verifyNoInteractions(event);
	}

	@Test
	void attachmentAccessExceptionCorrectHandledForUpdate() {
		var id = getEntityAndMockContext(Attachment_.CDS_NAME);
		var attachment = Attachments.create();
		attachment.setFileName("test.txt");
		attachment.setContent(null);
		attachment.setId(id);
		when(event.processEvent(any(), any(), any(), any())).thenThrow(new ServiceException(""));
		when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class))).thenReturn(
				List.of(attachment));

		List<CdsData> input = List.of(attachment);
		assertThrows(ServiceException.class, () -> cut.processBefore(updateContext, input));
	}

	@Test
	void existingDataFoundAndUsed() {
		var id = getEntityAndMockContext(RootTable_.CDS_NAME);
		var testStream = mock(InputStream.class);
		var root = fillRootData(testStream, id);
		var model = runtime.getCdsModel();
		var target = updateContext.getTarget();
		when(attachmentsReader.readAttachments(eq(model), eq(target), any(CqnFilterableStatement.class))).thenReturn(
				List.of(root));

		cut.processBefore(updateContext, List.of(root));

		verify(eventFactory).getEvent(eq(testStream), eq(null), cdsDataArgumentCaptor.capture());
		assertThat(cdsDataArgumentCaptor.getValue()).isEqualTo(root.getAttachments().get(0));
		cdsDataArgumentCaptor.getAllValues().clear();
		verify(event).processEvent(any(), eq(testStream), cdsDataArgumentCaptor.capture(), eq(updateContext));
	}

	@Test
	void noExistingDataFound() {
		var id = getEntityAndMockContext(RootTable_.CDS_NAME);
		when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class))).thenReturn(
				List.of(CdsData.create()));

		var testStream = mock(InputStream.class);
		var root = fillRootData(testStream, id);

		cut.processBefore(updateContext, List.of(root));

		verify(eventFactory).getEvent(testStream, null, CdsData.create());
	}

	@Test
	void noKeysNoException() {
		var id = getEntityAndMockContext(RootTable_.CDS_NAME);

		var root = RootTable.create();
		root.setId(id);
		var attachment = Attachments.create();
		var testStream = mock(InputStream.class);
		attachment.setContent(testStream);
		root.setAttachments(List.of(attachment));

		List<CdsData> roots = List.of(root);
		assertDoesNotThrow(() -> cut.processBefore(updateContext, roots));
	}

	@Test
	void selectIsUsedWithFilterAndWhere() {
		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.put(UP_ID, "test_full");
		attachment.setContent(mock(InputStream.class));
		var entityWithKeys = CQL.entity(Attachment_.CDS_NAME).matching(getAttachmentKeyMap(attachment));
		CqnUpdate update = Update.entity(entityWithKeys).byId("test");
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		mockTargetInUpdateContext(serviceEntity, update);

		cut.processBefore(updateContext, List.of(attachment));

		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(serviceEntity), selectCaptor.capture());
		var select = selectCaptor.getValue();
		assertThat(select.toString()).contains(getRefString("$key", "test"));
		assertThat(select.toString()).contains(getRefString(Attachment.ID, attachment.getId()));
		assertThat(select.toString()).contains(getRefString(UP_ID, (String) attachment.get(UP_ID)));
	}

	@Test
	void selectIsUsedWithFilter() {
		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.put(UP_ID, "test_filter");
		attachment.setContent(mock(InputStream.class));
		var entityWithKeys = CQL.entity(Attachment_.CDS_NAME).matching(getAttachmentKeyMap(attachment));
		CqnUpdate update = Update.entity(entityWithKeys);
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		mockTargetInUpdateContext(serviceEntity, update);

		cut.processBefore(updateContext, List.of(attachment));

		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(serviceEntity), selectCaptor.capture());
		var select = selectCaptor.getValue();
		assertThat(select.toString()).contains(getRefString(Attachment.ID, attachment.getId()));
		assertThat(select.toString()).contains(getRefString(UP_ID, (String) attachment.get(UP_ID)));
	}

	@Test
	void selectIsUsedWithWhere() {
		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.put(UP_ID, "test_where");
		attachment.setContent(mock(InputStream.class));
		CqnUpdate update = Update.entity(Attachment_.CDS_NAME).byId("test");
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		mockTargetInUpdateContext(serviceEntity, update);

		cut.processBefore(updateContext, List.of(attachment));

		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(serviceEntity), selectCaptor.capture());
		var select = selectCaptor.getValue();
		assertThat(select.toString()).doesNotContain(Attachment.ID);
		assertThat(select.toString()).doesNotContain(UP_ID);
		assertThat(select.toString()).contains(getRefString("$key", "test"));
	}

	@Test
	void selectIsUsedWithAttachmentId() {
		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.put(UP_ID, "test_up_id");
		attachment.setContent(mock(InputStream.class));
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		CqnUpdate update = Update.entity(Attachment_.class).where(entity -> entity.ID().eq(attachment.getId()));
		mockTargetInUpdateContext(serviceEntity, update);

		cut.processBefore(updateContext, List.of(attachment));

		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(serviceEntity), selectCaptor.capture());
		var select = selectCaptor.getValue();
		assertThat(select.toString()).contains(getRefString(Attachment.ID, attachment.getId()));
		assertThat(select.toString()).doesNotContain(UP_ID);
	}

	@Test
	void selectIsCorrectForMultipleAttachments() {
		var attachment1 = Attachments.create();
		attachment1.setId(UUID.randomUUID().toString());
		attachment1.put(UP_ID, "test_multiple 2");
		attachment1.setContent(mock(InputStream.class));
		var attachment2 = Attachments.create();
		attachment2.setId(UUID.randomUUID().toString());
		attachment2.put(UP_ID, "test_multiple 2");
		attachment2.setContent(mock(InputStream.class));
		CqnUpdate update = Update.entity(Attachment_.class).where(
				attachment -> attachment.ID().eq(attachment1.getId()).or(attachment.ID().eq(attachment2.getId())));
		var serviceEntity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
		mockTargetInUpdateContext(serviceEntity, update);

		cut.processBefore(updateContext, List.of(attachment1, attachment2));

		verify(attachmentsReader).readAttachments(eq(runtime.getCdsModel()), eq(serviceEntity), selectCaptor.capture());
		var select = selectCaptor.getValue();
		assertThat(select.toString()).contains(getOrCondition(attachment1.getId(), attachment2.getId()));
	}

	@Test
	void noContentInDataButAssociationIsChangedButNoDeleteCalled() {
		var id = getEntityAndMockContext(RootTable_.CDS_NAME);

		var root = RootTable.create();
		root.setId(id);
		root.setAttachments(Collections.emptyList());

		cut.processBefore(updateContext, List.of(root));

		verify(attachmentsReader).readAttachments(any(), any(), any(CqnFilterableStatement.class));
		verifyNoInteractions(eventFactory);
		verifyNoInteractions(attachmentService);
	}

	@Test
	void noContentInDataButAssociationIsChangedAndDeleteCalled() {
		var id = getEntityAndMockContext(RootTable_.CDS_NAME);

		var root = RootTable.create();
		root.setId(id);
		root.setAttachments(Collections.emptyList());

		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.setContent(mock(InputStream.class));
		attachment.setContentId("document id");
		var existingRoot = RootTable.create();
		existingRoot.setAttachments(List.of(attachment));
		when(attachmentsReader.readAttachments(any(), any(), any(CqnFilterableStatement.class))).thenReturn(
				List.of(existingRoot));
		when(updateContext.getUserInfo()).thenReturn(userInfo);

		cut.processBefore(updateContext, List.of(root));

		verify(attachmentsReader).readAttachments(any(), any(), any(CqnFilterableStatement.class));
		verifyNoInteractions(eventFactory);
		var deletionInputCaptor = ArgumentCaptor.forClass(MarkAsDeletedInput.class);
		verify(attachmentService).markAttachmentAsDeleted(deletionInputCaptor.capture());
		assertThat(deletionInputCaptor.getValue().contentId()).isEqualTo(attachment.getContentId());
		assertThat(deletionInputCaptor.getValue().userInfo()).isEqualTo(userInfo);
	}

	@Test
	void classHasCorrectAnnotation() {
		var updateHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(updateHandlerAnnotation.type()).containsOnly(ApplicationService.class);
		assertThat(updateHandlerAnnotation.value()).containsOnly("*");
	}

	@Test
	void methodHasCorrectAnnotations() throws NoSuchMethodException {
		var method = cut.getClass().getMethod("processBefore", CdsUpdateEventContext.class, List.class);

		var updateBeforeAnnotation = method.getAnnotation(Before.class);
		var updateHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

		assertThat(updateBeforeAnnotation.event()).isEmpty();
		assertThat(updateHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
	}

	private RootTable fillRootData(InputStream testStream, String id) {
		var root = RootTable.create();
		root.setId(id);
		var attachment = Attachments.create();
		attachment.setId(UUID.randomUUID().toString());
		attachment.put("up__ID", root.getId());
		attachment.setContent(testStream);
		root.setAttachments(List.of(attachment));
		return root;
	}

	private String getEntityAndMockContext(String cdsName) {
		var serviceEntity = runtime.getCdsModel().findEntity(cdsName);
		return mockTargetInUpdateContext(serviceEntity.orElseThrow());
	}

	private String mockTargetInUpdateContext(CdsEntity serviceEntity) {
		var id = UUID.randomUUID().toString();
		var update = Update.entity(serviceEntity.getQualifiedName()).where(entity -> entity.get("ID").eq(id));
		mockTargetInUpdateContext(serviceEntity, update);
		return id;
	}

	private void mockTargetInUpdateContext(CdsEntity serviceEntity, CqnUpdate update) {
		when(updateContext.getTarget()).thenReturn(serviceEntity);
		when(updateContext.getModel()).thenReturn(runtime.getCdsModel());
		when(updateContext.getCqn()).thenReturn(update);
	}

	private Map<String, Object> getAttachmentKeyMap(Attachments attachment) {
		return Map.of(Attachment.ID, attachment.getId(), "up__ID", attachment.get(UP_ID));
	}

	private String getRefString(String key, String value) {
		return """
				{"ref":["%s"]},"=",{"val":"%s"}
				""".formatted(key, value).replace(" ", "").replace("\n", "");
	}

	private String getOrCondition(String key1, String key2) {
		return """
				[{"ref":["ID"]},"=",{"val":"%s"},"or",{"ref":["ID"]},"=",{"val":"%s"}]
				""".formatted(key1, key2).replace(" ", "").replace("\n", "");
	}

}

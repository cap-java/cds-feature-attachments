/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.transaction.ListenerProvider;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.CreateAttachmentInput;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class ModifyAttachmentEventFactoryTest {

  private ModifyAttachmentEventFactory cut;
  private CreateAttachmentEvent createEvent;
  private UpdateAttachmentEvent updateEvent;
  private MarkAsDeletedAttachmentEvent deleteContentEvent;
  private DoNothingAttachmentEvent doNothingEvent;
  private AttachmentService attachmentService;
  private AttachmentService deleteAttachmentService;
  private ListenerProvider listenerProvider;
  private EventContext eventContext;
  private CdsEntity entity;
  private CdsData row;
  private static final String PREFIX = "avatar";

  @BeforeEach
  void setup() {
    attachmentService = mock(AttachmentService.class);
    deleteAttachmentService = mock(AttachmentService.class);
    listenerProvider = mock(ListenerProvider.class);
    createEvent = new CreateAttachmentEvent(attachmentService, listenerProvider);
    updateEvent =
        new UpdateAttachmentEvent(
            createEvent, new MarkAsDeletedAttachmentEvent(deleteAttachmentService));
    deleteContentEvent = new MarkAsDeletedAttachmentEvent(deleteAttachmentService);
    doNothingEvent = new DoNothingAttachmentEvent();

    cut =
        new ModifyAttachmentEventFactory(
            createEvent, updateEvent, deleteContentEvent, doNothingEvent);

    eventContext = mock(EventContext.class);
    entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn("test.Entity");
    var changeSetContext = mock(ChangeSetContext.class);
    when(eventContext.getChangeSetContext()).thenReturn(changeSetContext);
    var target = mock(CdsEntity.class);
    when(eventContext.getTarget()).thenReturn(target);
    when(target.getQualifiedName()).thenReturn("test.Entity");
    var userInfo = mock(UserInfo.class);
    when(eventContext.getUserInfo()).thenReturn(userInfo);
    row = CdsData.create();
  }

  @Test
  void allNullNothingToDo() {
    var event = cut.getEvent(null, null, Attachments.create());

    assertThat(event).isEqualTo(doNothingEvent);
  }

  @Test
  void contentIdsNullContentFilledReturnedCreateEvent() {
    var event = cut.getEvent(mock(InputStream.class), null, Attachments.create());

    assertThat(event).isEqualTo(createEvent);
  }

  @Test
  void contentIdNullButtExistingNotNullReturnsDelete() {
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, "someValue");

    var event = cut.getEvent(null, null, data);

    assertThat(event).isEqualTo(deleteContentEvent);
  }

  @Test
  void contentIdsSameContentFillReturnsDoNothing() {
    var contentId = "test ID";
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, contentId);

    var event = cut.getEvent(mock(InputStream.class), contentId, data);

    assertThat(event).isEqualTo(doNothingEvent);
  }

  @ParameterizedTest
  @ValueSource(strings = {"some document Id"})
  @NullSource
  @EmptySource
  void contentIdNotPresentAndExistingNotNullReturnsUpdateEvent(String contentId) {
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, "someValue");

    var event = cut.getEvent(mock(InputStream.class), contentId, data);

    assertThat(event).isEqualTo(updateEvent);
  }

  @Test
  void contentIdsSameContentNullReturnsNothingToDo() {
    var contentId = "test ID";
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, contentId);

    var event = cut.getEvent(null, contentId, data);

    assertThat(event).isEqualTo(doNothingEvent);
  }

  @ParameterizedTest
  @ValueSource(strings = {"some document Id"})
  @NullSource
  @EmptySource
  void contentIdNotPresentAndExistingNotNullReturnsDeleteEvent(String contentId) {
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, "someValue");

    var event = cut.getEvent(null, contentId, data);

    assertThat(event).isEqualTo(deleteContentEvent);
  }

  @Test
  void contentIdPresentAndExistingIdIsNullReturnsNothingToDo() {
    var event = cut.getEvent(mock(InputStream.class), "test", Attachments.create());

    assertThat(event).isEqualTo(doNothingEvent);
  }

  @ParameterizedTest
  //	@ValueSource(strings = {"some document Id"})
  @NullSource
  //	@EmptySource
  void contentIdNotPresentAndExistingNullReturnsCreateEvent(String contentId) {
    var event = cut.getEvent(mock(InputStream.class), contentId, Attachments.create());

    assertThat(event).isEqualTo(createEvent);
  }

  @ParameterizedTest
  @ValueSource(strings = {"some document Id"})
  @NullSource
  @EmptySource
  void contentIdNotPresentAndExistingNullReturnsDoNothingEvent(String contentId) {
    var event = cut.getEvent(null, contentId, Attachments.create());

    assertThat(event).isEqualTo(doNothingEvent);
  }

  @Test
  void contentIdPresentButNullAndExistingNotNullReturnsUpdateEvent() {
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, "someValue");

    var event = cut.getEvent(mock(InputStream.class), null, data);

    assertThat(event).isEqualTo(updateEvent);
  }

  @Test
  void updateIfContentIdDifferentButContentProvided() {
    var data = Attachments.create();
    data.put(Attachments.CONTENT_ID, "existing");

    var event = cut.getEvent(mock(InputStream.class), "someValue", data);

    assertThat(event).isEqualTo(updateEvent);
  }

  @Test
  void processInlineEvent_doNothing_returnsContentUnchanged() {
    var content = mock(InputStream.class);
    var existing = Attachments.create();
    existing.setContentId("same-id");
    Map<String, Object> keys = Map.of("ID", "k1");

    InputStream result =
        cut.processInlineEvent(
            content, "same-id", existing, eventContext, entity, keys, row, PREFIX);

    assertThat(result).isSameAs(content);
  }

  @Test
  void processInlineEvent_create_callsAttachmentService() {
    var content = mock(InputStream.class);
    var existing = Attachments.create();
    existing.put(Attachments.MIME_TYPE, "image/png");
    existing.put(Attachments.FILE_NAME, "avatar.png");
    Map<String, Object> keys = Map.of("ID", "k1");

    stubCreateAttachment(
        new AttachmentModificationResult(false, "new-cid", "Clean", Instant.now()));

    InputStream result =
        cut.processInlineEvent(content, null, existing, eventContext, entity, keys, row, PREFIX);

    verify(attachmentService).createAttachment(any());
    assertThat(existing.getContentId()).isEqualTo("new-cid");
    assertThat(existing.getStatus()).isEqualTo("Clean");
    assertThat(existing.getScannedAt()).isNotNull();
    assertThat(result).isNull();
  }

  @Test
  void processInlineEvent_create_readsMetadataFromRowPrefixedFields() {
    var content = mock(InputStream.class);
    var existing = Attachments.create();
    existing.put(Attachments.MIME_TYPE, "old/type");
    existing.put(Attachments.FILE_NAME, "old.txt");
    Map<String, Object> keys = Map.of("ID", "k1");

    row.put("avatar_mimeType", "image/png");
    row.put("avatar_fileName", "new-avatar.png");

    stubCreateAttachment(
        new AttachmentModificationResult(false, "new-cid", "Clean", Instant.now()));

    ArgumentCaptor<CreateAttachmentInput> inputCaptor =
        ArgumentCaptor.forClass(CreateAttachmentInput.class);

    cut.processInlineEvent(content, null, existing, eventContext, entity, keys, row, PREFIX);

    verify(attachmentService).createAttachment(inputCaptor.capture());
    assertThat(inputCaptor.getValue().fileName()).isEqualTo("new-avatar.png");
    assertThat(inputCaptor.getValue().mimeType()).isEqualTo("image/png");
  }

  @Test
  void processInlineEvent_create_internalStored_returnsContent() {
    var content = mock(InputStream.class);
    var existing = Attachments.create();
    Map<String, Object> keys = Map.of("ID", "k1");

    stubCreateAttachment(new AttachmentModificationResult(true, "new-cid", "Clean", null));

    InputStream result =
        cut.processInlineEvent(content, null, existing, eventContext, entity, keys, row, PREFIX);

    assertThat(result).isSameAs(content);
    assertThat(existing.getScannedAt()).isNull();
  }

  @Test
  void processInlineEvent_update_deletesOldAndCreatesNew() {
    var content = mock(InputStream.class);
    var existing = Attachments.create();
    existing.setContentId("old-cid");
    Map<String, Object> keys = Map.of("ID", "k1");

    stubCreateAttachment(new AttachmentModificationResult(false, "new-cid", "Clean", null));

    InputStream result =
        cut.processInlineEvent(content, null, existing, eventContext, entity, keys, row, PREFIX);

    verify(deleteAttachmentService).markAttachmentAsDeleted(any(MarkAsDeletedInput.class));
    verify(attachmentService).createAttachment(any());
    assertThat(existing.getContentId()).isEqualTo("new-cid");
    assertThat(result).isNull();
  }

  @Test
  void processInlineEvent_delete_marksAsDeletedAndClearsFields() {
    var existing = Attachments.create();
    existing.setContentId("old-cid");
    existing.setStatus("Clean");
    existing.setScannedAt(Instant.now());
    Map<String, Object> keys = Map.of("ID", "k1");

    InputStream result =
        cut.processInlineEvent(null, null, existing, eventContext, entity, keys, row, PREFIX);

    verify(deleteAttachmentService).markAttachmentAsDeleted(any(MarkAsDeletedInput.class));
    assertThat(existing.getContentId()).isNull();
    assertThat(existing.getStatus()).isNull();
    assertThat(existing.getScannedAt()).isNull();
    assertThat(result).isNull();
  }

  @Test
  void processInlineEvent_deleteWithDraftPatchEvent_skipsMarkAsDeleted() {
    var existing = Attachments.create();
    existing.setContentId("old-cid");
    Map<String, Object> keys = Map.of("ID", "k1");
    when(eventContext.getEvent()).thenReturn(DraftService.EVENT_DRAFT_PATCH);

    InputStream result =
        cut.processInlineEvent(null, null, existing, eventContext, entity, keys, row, PREFIX);

    verifyNoInteractions(deleteAttachmentService);
    assertThat(existing.getContentId()).isNull();
    assertThat(result).isNull();
  }

  @Test
  void processInlineEvent_updateWithDraftPatchEvent_skipsMarkAsDeletedButCreates() {
    var content = mock(InputStream.class);
    var existing = Attachments.create();
    existing.setContentId("old-cid");
    Map<String, Object> keys = Map.of("ID", "k1");
    when(eventContext.getEvent()).thenReturn(DraftService.EVENT_DRAFT_PATCH);

    stubCreateAttachment(new AttachmentModificationResult(false, "new-cid", "Clean", null));

    InputStream result =
        cut.processInlineEvent(content, null, existing, eventContext, entity, keys, row, PREFIX);

    verifyNoInteractions(deleteAttachmentService);
    verify(attachmentService).createAttachment(any());
    assertThat(existing.getContentId()).isEqualTo("new-cid");
    assertThat(result).isNull();
  }

  @Test
  void processInlineEvent_deleteWithNullContentId_skipsMarkAsDeleted() {
    var existing = Attachments.create();
    Map<String, Object> keys = Map.of("ID", "k1");

    InputStream result =
        cut.processInlineEvent(null, null, existing, eventContext, entity, keys, row, PREFIX);

    assertThat(result).isNull();
    verifyNoInteractions(deleteAttachmentService);
  }

  private void stubCreateAttachment(AttachmentModificationResult result) {
    when(attachmentService.createAttachment(any())).thenReturn(result);
    when(listenerProvider.provideListener(any(), any())).thenReturn(mock(ChangeSetListener.class));
    when(eventContext.getCdsRuntime()).thenReturn(mock(CdsRuntime.class));
  }
}

/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageSetter;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DraftActiveAttachmentsHandlerTest {

  private static final String DRAFT_ROOTS_ENTITY = "unit.test.TestService.RootTable";
  private static final String EVENT_ITEMS_ENTITY = "unit.test.TestService.EventItems";

  private static CdsRuntime runtime;

  private DraftActiveAttachmentsHandler cut;
  private ThreadDataStorageSetter threadLocalSetter;
  private PersistenceService persistence;
  private ArgumentCaptor<Runnable> runnableCaptor;
  private Messages messages;
  private Message messageMock;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    threadLocalSetter = mock(ThreadDataStorageSetter.class);
    persistence = mock(PersistenceService.class);
    cut = new DraftActiveAttachmentsHandler(threadLocalSetter, persistence);
    runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    messages = mock(Messages.class);
    messageMock = mock(Message.class);
    when(messageMock.target(any(String.class))).thenReturn(messageMock);
    when(messages.warn(any(String.class), any(Object[].class))).thenReturn(messageMock);
    when(messages.error(any(String.class), any(Object[].class))).thenReturn(messageMock);
  }

  // -------------------------------------------------------------------------
  // processDraftSave (@On)
  // -------------------------------------------------------------------------

  @Test
  void setterCalled() {
    var context = mock(DraftSaveEventContext.class);
    var entity = getRootEntity();
    when(context.getTarget()).thenReturn(entity);

    cut.processDraftSave(context);

    verify(threadLocalSetter).set(eq(true), runnableCaptor.capture());
    runnableCaptor.getValue().run();
    verify(context).proceed();
  }

  // -------------------------------------------------------------------------
  // validateItemCountBeforeSave (@Before) – entity without annotations
  // -------------------------------------------------------------------------

  @Test
  void validateItemCount_entityWithoutAnnotations_noPersistenceQuery() {
    CdsEntity entity = getRootEntity();
    var context = buildSaveContext(entity);

    assertDoesNotThrow(() -> cut.validateItemCountBeforeSave(context));

    verifyNoInteractions(persistence);
  }

  // -------------------------------------------------------------------------
  // validateItemCountBeforeSave (@Before) – entity with annotations
  // -------------------------------------------------------------------------

  @Test
  void validateItemCount_withinBounds_noError() {
    CdsEntity entity = getEventItemsEntity();
    var context = buildSaveContext(entity);
    when(context.getMessages()).thenReturn(messages);

    // minMaxAttachments requires [2,5] → 3 items; minAttachments requires ≥1 → 1 item
    // maxAttachments allows ≤3 → 0 items; propertyRefAttachments (stock ref) → no stock → skip
    mockDraftDbResult(context, java.util.Map.of("minMaxAttachments", 3, "minAttachments", 1));

    assertDoesNotThrow(() -> cut.validateItemCountBeforeSave(context));
  }

  @Test
  void validateItemCount_tooFewItems_throwsError() {
    CdsEntity entity = getEventItemsEntity();
    var context = buildSaveContext(entity);
    when(context.getMessages()).thenReturn(messages);

    // minMaxAttachments requires min 2; return 1 item from DB; minAttachments satisfied
    mockDraftDbResult(context, java.util.Map.of("minMaxAttachments", 1, "minAttachments", 1));
    org.mockito.Mockito.doThrow(new ServiceException("min items violated"))
        .when(messages)
        .throwIfError();

    assertThrows(ServiceException.class, () -> cut.validateItemCountBeforeSave(context));
    verify(messages).error(any(String.class), eq(1L), eq(2L));
  }

  @Test
  void validateItemCount_tooManyItems_throwsError() {
    CdsEntity entity = getEventItemsEntity();
    var context = buildSaveContext(entity);
    when(context.getMessages()).thenReturn(messages);

    // minMaxAttachments has max 5; return 7 items from DB; minAttachments satisfied
    mockDraftDbResult(context, java.util.Map.of("minMaxAttachments", 7, "minAttachments", 1));
    org.mockito.Mockito.doThrow(new ServiceException("max items violated"))
        .when(messages)
        .throwIfError();

    assertThrows(ServiceException.class, () -> cut.validateItemCountBeforeSave(context));
    verify(messages).error(any(String.class), eq(7L), eq(5L));
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private CdsEntity getRootEntity() {
    return runtime.getCdsModel().findEntity(DRAFT_ROOTS_ENTITY).orElseThrow();
  }

  private CdsEntity getEventItemsEntity() {
    return runtime.getCdsModel().findEntity(EVENT_ITEMS_ENTITY).orElseThrow();
  }

  private DraftSaveEventContext buildSaveContext(CdsEntity entity) {
    var context = mock(DraftSaveEventContext.class);
    when(context.getTarget()).thenReturn(entity);
    when(context.getCqn()).thenReturn(mock(com.sap.cds.ql.cqn.CqnSelect.class));
    when(context.getModel()).thenReturn(runtime.getCdsModel());
    return context;
  }

  /**
   * Mocks the persistence service to return a root row whose compositions have the given counts.
   * Compositions not in the map are absent from the result (count treated as 0).
   */
  private void mockDraftDbResult(
      DraftSaveEventContext context, java.util.Map<String, Integer> compCounts) {
    CdsData rootRow = CdsData.create();
    for (var entry : compCounts.entrySet()) {
      List<CdsData> attachments = new ArrayList<>();
      for (int i = 0; i < entry.getValue(); i++) {
        Attachments att = Attachments.create();
        att.setId(UUID.randomUUID().toString());
        attachments.add(att);
      }
      rootRow.put(entry.getKey(), attachments);
    }

    Result result = mock(Result.class);
    when(result.listOf(CdsData.class)).thenReturn(List.of(rootRow));
    when(persistence.run(any(com.sap.cds.ql.cqn.CqnSelect.class))).thenReturn(result);
  }
}

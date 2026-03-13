/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.Result;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsType;
import com.sap.cds.services.draft.DraftNewEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftSaveEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.persistence.PersistenceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DraftItemCountValidationHandler}.
 *
 * <p>The draft handler validates item count constraints:
 *
 * <ul>
 *   <li>DRAFT_SAVE -> ERROR severity (rejecting the activation)
 *   <li>DRAFT_PATCH -> WARNING severity (allowing draft to be saved in invalid state)
 *   <li>DRAFT_NEW -> WARNING severity (allowing draft to be created in invalid state)
 * </ul>
 */
class DraftItemCountValidationHandlerTest {

  private DraftItemCountValidationHandler cut;
  private PersistenceService persistenceService;
  private DraftSaveEventContext draftSaveContext;
  private DraftPatchEventContext draftPatchContext;
  private DraftNewEventContext draftNewContext;
  private Messages messages;
  private Message message;
  private CdsEntity entity;
  private CdsModel model;

  @BeforeEach
  void setup() {
    persistenceService = mock(PersistenceService.class);
    cut = new DraftItemCountValidationHandler(persistenceService);

    draftSaveContext = mock(DraftSaveEventContext.class);
    draftPatchContext = mock(DraftPatchEventContext.class);
    draftNewContext = mock(DraftNewEventContext.class);
    messages = mock(Messages.class);
    message = mock(Message.class);
    entity = mock(CdsEntity.class);
    model = mock(CdsModel.class);

    when(draftSaveContext.getMessages()).thenReturn(messages);
    when(draftSaveContext.getTarget()).thenReturn(entity);
    when(draftSaveContext.getModel()).thenReturn(model);
    // Use a real CqnSelect so that ref() returns a proper CqnStructuredTypeRef
    CqnSelect draftCqn = Select.from("TestDraftService.DraftRoots_drafts");
    when(draftSaveContext.getCqn()).thenReturn(draftCqn);

    when(draftPatchContext.getMessages()).thenReturn(messages);
    when(draftPatchContext.getTarget()).thenReturn(entity);
    when(draftPatchContext.getModel()).thenReturn(model);

    when(draftNewContext.getMessages()).thenReturn(messages);
    when(draftNewContext.getTarget()).thenReturn(entity);
    when(draftNewContext.getModel()).thenReturn(model);

    when(messages.error(anyString(), anyInt(), anyString())).thenReturn(message);
    when(messages.warn(anyString(), anyInt(), anyString())).thenReturn(message);
    when(messages.info(anyString(), anyInt(), anyString())).thenReturn(message);
    when(message.target(anyString())).thenReturn(message);

    when(entity.getQualifiedName()).thenReturn("TestDraftService.DraftRoots");
  }

  // ============================
  // Helper methods
  // ============================

  @SuppressWarnings("unchecked")
  private CdsElement createAnnotatedCompositionElement(
      String elementName, Integer maxItems, Integer minItems) {
    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn(elementName);

    CdsType type = mock(CdsType.class);
    when(type.isAssociation()).thenReturn(true);
    CdsAssociationType assocType = mock(CdsAssociationType.class);
    when(assocType.isComposition()).thenReturn(true);
    when(type.as(CdsAssociationType.class)).thenReturn(assocType);
    when(element.getType()).thenReturn(type);

    if (maxItems != null) {
      CdsAnnotation<Object> maxAnnotation = mock(CdsAnnotation.class);
      when(maxAnnotation.getValue()).thenReturn(maxItems);
      when(element.findAnnotation("Validation.MaxItems")).thenReturn(Optional.of(maxAnnotation));
    } else {
      when(element.findAnnotation("Validation.MaxItems")).thenReturn(Optional.empty());
    }

    if (minItems != null) {
      CdsAnnotation<Object> minAnnotation = mock(CdsAnnotation.class);
      when(minAnnotation.getValue()).thenReturn(minItems);
      when(element.findAnnotation("Validation.MinItems")).thenReturn(Optional.of(minAnnotation));
    } else {
      when(element.findAnnotation("Validation.MinItems")).thenReturn(Optional.empty());
    }

    return element;
  }

  private void mockEntityElements(CdsElement... elements) {
    when(entity.elements()).thenAnswer(invocation -> Stream.of(elements));
  }

  private List<CdsData> createItems(int count) {
    List<CdsData> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(Attachments.create());
    }
    return items;
  }

  private CdsData createRootWithCompositionItems(String compositionName, int itemCount) {
    CdsData root = CdsData.create();
    root.put(compositionName, createItems(itemCount));
    return root;
  }

  private CdsElement createNonCompositionElement(String elementName) {
    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn(elementName);

    CdsType type = mock(CdsType.class);
    when(type.isAssociation()).thenReturn(false);
    when(element.getType()).thenReturn(type);

    when(element.findAnnotation(anyString())).thenReturn(Optional.empty());
    return element;
  }

  private void mockPersistenceResult(List<CdsData> data) {
    Result result = mock(Result.class);
    when(result.listOf(CdsData.class)).thenReturn(data);
    when(persistenceService.run(any(CqnSelect.class))).thenReturn(result);
  }

  // ============================
  // DRAFT_SAVE tests (ERROR severity, reads from persistence)
  // ============================

  @Nested
  class DraftSaveTests {

    @Test
    void draftSaveWithMaxItemsExceeded_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 5)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages).error(anyString(), eq(3), eq("attachments"));
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void draftSaveWithMinItemsNotMet_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 1)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages).error(anyString(), eq(3), eq("attachments"));
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void draftSaveWithinLimits_noMessages() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 10, 1);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 5)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void draftSaveEmptyData_noMessages() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);
      mockPersistenceResult(Collections.emptyList());

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftSaveErrorTargetsCompositionProperty() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 5)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages).error(anyString(), eq(2), eq("attachments"));
      verify(message).target("attachments");
    }

    @Test
    void draftSaveExactlyAtMaxItems_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 3)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftSaveExactlyAtMinItems_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 2);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 2)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftSaveMinItemsViolated_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 5, 3);
      mockEntityElements(compositionEl);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 0)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages).error(anyString(), eq(3), eq("attachments"));
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }
  }

  // ============================
  // DRAFT_PATCH tests (WARNING severity)
  // ============================

  @Nested
  class DraftPatchTests {

    @Test
    void draftPatchWithMaxItemsExceeded_producesWarning() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftPatch(
          draftPatchContext, List.of(createRootWithCompositionItems("attachments", 5)));

      verify(messages).warn(anyString(), eq(3), eq("attachments"));
      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftPatchWithMinItemsNotMet_producesWarning() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftPatch(
          draftPatchContext, List.of(createRootWithCompositionItems("attachments", 1)));

      verify(messages).warn(anyString(), eq(3), eq("attachments"));
      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftPatchWithinLimits_noMessages() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 10, 1);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftPatch(
          draftPatchContext, List.of(createRootWithCompositionItems("attachments", 5)));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void draftPatchWarningTargetsCompositionProperty() {
      var compositionEl = createAnnotatedCompositionElement("documents", 2, null);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftPatch(
          draftPatchContext, List.of(createRootWithCompositionItems("documents", 5)));

      verify(messages).warn(anyString(), eq(2), eq("documents"));
      verify(message).target("documents");
    }

    @Test
    void draftPatchEmptyData_noMessages() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftPatch(draftPatchContext, Collections.emptyList());

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }
  }

  // ============================
  // DRAFT_NEW tests (WARNING severity)
  // ============================

  @Nested
  class DraftNewTests {

    @Test
    void draftNewWithMaxItemsExceeded_producesWarning() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftNew(
          draftNewContext, List.of(createRootWithCompositionItems("attachments", 5)));

      verify(messages).warn(anyString(), eq(2), eq("attachments"));
      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftNewWithMinItemsNotMet_producesWarning() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftNew(
          draftNewContext, List.of(createRootWithCompositionItems("attachments", 0)));

      verify(messages).warn(anyString(), eq(3), eq("attachments"));
      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void draftNewWithinLimits_noMessages() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 10, 1);
      mockEntityElements(compositionEl);

      cut.processBeforeDraftNew(
          draftNewContext, List.of(createRootWithCompositionItems("attachments", 5)));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }
  }

  // ============================
  // Handler annotation tests
  // ============================

  @Nested
  class HandlerAnnotationTests {

    @Test
    void classHasCorrectServiceAnnotation() {
      var serviceAnnotation = cut.getClass().getAnnotation(ServiceName.class);

      assertThat(serviceAnnotation.type()).containsOnly(DraftService.class);
      assertThat(serviceAnnotation.value()).containsOnly("*");
    }

    @Test
    void processBeforeDraftSaveHasCorrectAnnotations() throws NoSuchMethodException {
      var method =
          cut.getClass().getDeclaredMethod("processBeforeDraftSave", DraftSaveEventContext.class);

      var beforeAnnotation = method.getAnnotation(Before.class);
      var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

      assertThat(beforeAnnotation).isNotNull();
      assertThat(beforeAnnotation.event()).containsExactly(DraftService.EVENT_DRAFT_SAVE);
      assertThat(handlerOrderAnnotation).isNotNull();
      assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
    }

    @Test
    void processBeforeDraftPatchHasCorrectAnnotations() throws NoSuchMethodException {
      var method =
          cut.getClass()
              .getDeclaredMethod(
                  "processBeforeDraftPatch", DraftPatchEventContext.class, List.class);

      var beforeAnnotation = method.getAnnotation(Before.class);
      var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

      assertThat(beforeAnnotation).isNotNull();
      assertThat(beforeAnnotation.event()).containsExactly(DraftService.EVENT_DRAFT_PATCH);
      assertThat(handlerOrderAnnotation).isNotNull();
      assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
    }

    @Test
    void processBeforeDraftNewHasCorrectAnnotations() throws NoSuchMethodException {
      var method =
          cut.getClass()
              .getDeclaredMethod("processBeforeDraftNew", DraftNewEventContext.class, List.class);

      var beforeAnnotation = method.getAnnotation(Before.class);
      var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

      assertThat(beforeAnnotation).isNotNull();
      assertThat(beforeAnnotation.event()).containsExactly(DraftService.EVENT_DRAFT_NEW);
      assertThat(handlerOrderAnnotation).isNotNull();
      assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
    }
  }

  // ============================
  // No annotation tests
  // ============================

  @Nested
  class NoAnnotationTests {

    @Test
    void noAnnotatedCompositions_draftSave_noValidation() {
      var element = createNonCompositionElement("title");
      mockEntityElements(element);
      mockPersistenceResult(List.of(createRootWithCompositionItems("attachments", 100)));

      cut.processBeforeDraftSave(draftSaveContext);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void noAnnotatedCompositions_draftPatch_noValidation() {
      var element = createNonCompositionElement("title");
      mockEntityElements(element);

      cut.processBeforeDraftPatch(
          draftPatchContext, List.of(createRootWithCompositionItems("attachments", 100)));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void compositionNotInPayload_noMessages() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);
      var root = CdsData.create();

      cut.processBeforeDraftPatch(draftPatchContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }
  }
}

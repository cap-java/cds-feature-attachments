/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsType;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsUpdateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Message;
import com.sap.cds.services.messages.Messages;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ItemCountValidationHandlerTest {

  private ItemCountValidationHandler cut;
  private AttachmentsReader attachmentsReader;
  private CdsCreateEventContext createContext;
  private CdsUpdateEventContext updateContext;
  private Messages messages;
  private Message message;
  private CdsEntity entity;
  private CdsModel model;

  @BeforeEach
  void setup() {
    attachmentsReader = mock(AttachmentsReader.class);
    cut = new ItemCountValidationHandler(attachmentsReader);

    createContext = mock(CdsCreateEventContext.class);
    updateContext = mock(CdsUpdateEventContext.class);
    messages = mock(Messages.class);
    message = mock(Message.class);
    entity = mock(CdsEntity.class);
    model = mock(CdsModel.class);

    when(createContext.getMessages()).thenReturn(messages);
    when(createContext.getTarget()).thenReturn(entity);
    when(createContext.getModel()).thenReturn(model);

    when(updateContext.getMessages()).thenReturn(messages);
    when(updateContext.getTarget()).thenReturn(entity);
    when(updateContext.getModel()).thenReturn(model);
    CqnUpdate cqnUpdate = Update.entity("TestService.RootTable").byId("test-id");
    when(updateContext.getCqn()).thenReturn(cqnUpdate);

    when(messages.error(anyString(), anyInt(), anyString())).thenReturn(message);
    when(messages.warn(anyString(), anyInt(), anyString())).thenReturn(message);
    when(messages.info(anyString(), anyInt(), anyString())).thenReturn(message);
    when(message.target(anyString())).thenReturn(message);

    when(entity.getQualifiedName()).thenReturn("TestService.RootTable");
  }

  // ============================
  // Helper methods
  // ============================

  @SuppressWarnings("unchecked")
  private CdsElement createAnnotatedCompositionElement(
      String elementName, Integer maxItems, Integer minItems) {
    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn(elementName);

    // Mock the association type chain
    CdsType type = mock(CdsType.class);
    when(type.isAssociation()).thenReturn(true);
    CdsAssociationType assocType = mock(CdsAssociationType.class);
    when(assocType.isComposition()).thenReturn(true);
    when(type.as(CdsAssociationType.class)).thenReturn(assocType);
    when(element.getType()).thenReturn(type);

    if (maxItems != null) {
      CdsAnnotation<Object> maxAnnotation = mock(CdsAnnotation.class);
      when(maxAnnotation.getValue()).thenReturn(maxItems);
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.of(maxAnnotation));
    } else {
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.empty());
    }

    if (minItems != null) {
      CdsAnnotation<Object> minAnnotation = mock(CdsAnnotation.class);
      when(minAnnotation.getValue()).thenReturn(minItems);
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MIN_ITEMS))
          .thenReturn(Optional.of(minAnnotation));
    } else {
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MIN_ITEMS))
          .thenReturn(Optional.empty());
    }

    return element;
  }

  private CdsElement createNonAnnotatedElement(String elementName, boolean isComposition) {
    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn(elementName);

    CdsType type = mock(CdsType.class);
    if (isComposition) {
      when(type.isAssociation()).thenReturn(true);
      CdsAssociationType assocType = mock(CdsAssociationType.class);
      when(assocType.isComposition()).thenReturn(true);
      when(type.as(CdsAssociationType.class)).thenReturn(assocType);
    } else {
      when(type.isAssociation()).thenReturn(false);
    }
    when(element.getType()).thenReturn(type);

    when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
        .thenReturn(Optional.empty());
    when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MIN_ITEMS))
        .thenReturn(Optional.empty());
    return element;
  }

  private void mockEntityElements(CdsElement... elements) {
    // entity.elements() returns a stream; it may be called multiple times so we use thenAnswer
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

  // ============================
  // CREATE tests
  // ============================

  @Nested
  class CreateTests {

    @Test
    void createWithItemsWithinMaxItemsLimit_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 5, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 3);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void createWithItemsExceedingMaxItems_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
      verify(message).target(eq("attachments"));
    }

    @Test
    void createWithItemsMeetingMinItems_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 2);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 3);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void createWithItemsBelowMinItems_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 1);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
      verify(message).target(eq("attachments"));
    }

    @Test
    void createWithBothMaxItemsAndMinItemsValid_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 10, 2);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void createWithMaxItemsExceeded_producesErrorForMax() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void createWithMinItemsNotMet_producesErrorForMin() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 10, 3);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 1);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void createWithItemsExactlyAtMaxItems_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 3);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void createWithItemsExactlyAtMinItems_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 3);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }
  }

  // ============================
  // No annotation tests
  // ============================

  @Nested
  class NoAnnotationTests {

    @Test
    void noAnnotationPresent_noValidationPerformed() {
      var compositionEl = createNonAnnotatedElement("attachments", true);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 100);

      cut.processBeforeCreate(createContext, List.of(root));

      verifyNoInteractions(messages);
    }

    @Test
    void updateNoAnnotationPresent_noValidationPerformed() {
      var compositionEl = createNonAnnotatedElement("attachments", true);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 100);

      cut.processBeforeUpdate(updateContext, List.of(root));

      verifyNoInteractions(messages);
    }

    @Test
    void onlyMaxItemsAnnotation_onlyMaxChecked() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void onlyMinItemsAnnotation_onlyMinChecked() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 1);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void nonCompositionElement_noValidation() {
      var element = createNonAnnotatedElement("title", false);
      mockEntityElements(element);
      var root = CdsData.create();
      root.put("title", "some title");

      cut.processBeforeCreate(createContext, List.of(root));

      verifyNoInteractions(messages);
    }
  }

  // ============================
  // Edge case tests
  // ============================

  @Nested
  class EdgeCaseTests {

    @Test
    void maxItemsAnnotationValueIsZero_anyItemExceedsLimit() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 0, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 1);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(0), eq("attachments"));
    }

    @Test
    void maxItemsAnnotationValueIsZero_emptyListPasses() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 0, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 0);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void minItemsAnnotationValueIsZero_emptyCompositionPasses() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 0);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 0);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void emptyCompositionInPayload_checkAgainstMinItems() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 2);
      mockEntityElements(compositionEl);
      var root = CdsData.create();
      root.put("attachments", Collections.emptyList());

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(2), eq("attachments"));
    }

    @Test
    void compositionNotInPayload_noValidation() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);
      var root = CdsData.create();
      // "attachments" key not set in data at all

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void compositionValueIsNotAList_noValidation() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);
      var root = CdsData.create();
      // Set composition to a non-List value (e.g. a single Map)
      root.put("attachments", CdsData.create());

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }
  }

  // ============================
  // Multiple compositions tests
  // ============================

  @Nested
  class MultipleCompositionsTests {

    @Test
    void multipleCompositions_eachValidatedIndependently() {
      var attachmentsEl = createAnnotatedCompositionElement("attachments", 3, null);
      var documentsEl = createAnnotatedCompositionElement("documents", 2, null);
      mockEntityElements(attachmentsEl, documentsEl);

      var root = CdsData.create();
      root.put("attachments", createItems(2)); // within limit
      root.put("documents", createItems(5)); // exceeds limit

      cut.processBeforeCreate(createContext, List.of(root));

      // Only documents should trigger error (5 > 2)
      verify(messages).error(anyString(), eq(2), eq("documents"));
      verify(messages, never()).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void multipleCompositions_bothExceed() {
      var attachmentsEl = createAnnotatedCompositionElement("attachments", 1, null);
      var documentsEl = createAnnotatedCompositionElement("documents", 1, null);
      mockEntityElements(attachmentsEl, documentsEl);

      var root = CdsData.create();
      root.put("attachments", createItems(3));
      root.put("documents", createItems(3));

      cut.processBeforeCreate(createContext, List.of(root));

      // Both should trigger errors
      verify(messages).error(anyString(), eq(1), eq("attachments"));
      verify(messages).error(anyString(), eq(1), eq("documents"));
    }
  }

  // ============================
  // UPDATE tests
  // ============================

  @Nested
  class UpdateTests {

    @Test
    void updateItemsExceedingMaxItems_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);

      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeUpdate(updateContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void updateWithinLimits_noError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 5, null);
      mockEntityElements(compositionEl);

      var root = createRootWithCompositionItems("attachments", 2);

      cut.processBeforeUpdate(updateContext, List.of(root));

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void updateBelowMinItems_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);

      var root = createRootWithCompositionItems("attachments", 1);

      cut.processBeforeUpdate(updateContext, List.of(root));

      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }
  }

  // ============================
  // Message target tests
  // ============================

  @Nested
  class MessageTargetTests {

    @Test
    void errorMessageContainsCorrectTargetPath() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(2), eq("attachments"));
      verify(message).target(eq("attachments"));
    }

    @Test
    void errorMessageForMinItemsContainsCorrectTargetPath() {
      var compositionEl = createAnnotatedCompositionElement("items", null, 2);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("items", 0);

      cut.processBeforeCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq(2), eq("items"));
      verify(message).target(eq("items"));
    }
  }

  // ============================
  // I18n message key tests
  // ============================

  @Nested
  class I18nMessageKeyTests {

    @Test
    void errorMessageUsesMaxItemsExceededKey() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      when(entity.getQualifiedName()).thenReturn("TestService.RootTable");
      var root = createRootWithCompositionItems("attachments", 5);

      cut.processBeforeCreate(createContext, List.of(root));

      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      verify(messages).error(keyCaptor.capture(), eq(2), eq("attachments"));
      String key = keyCaptor.getValue();
      assertThat(key).contains("MaxItems");
    }

    @Test
    void errorMessageUsesMinItemsNotMetKey() {
      var compositionEl = createAnnotatedCompositionElement("attachments", null, 3);
      mockEntityElements(compositionEl);
      when(entity.getQualifiedName()).thenReturn("TestService.RootTable");
      var root = createRootWithCompositionItems("attachments", 1);

      cut.processBeforeCreate(createContext, List.of(root));

      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      verify(messages).error(keyCaptor.capture(), eq(3), eq("attachments"));
      String key = keyCaptor.getValue();
      assertThat(key).contains("MinItems");
    }

    @Test
    void resolveMessageKeyFallsBackToBaseKey() {
      String result =
          ItemCountValidationHandler.resolveMessageKey(
              ItemCountValidationHandler.MSG_KEY_MAX_ITEMS_EXCEEDED,
              "NonExistentEntity",
              "nonExistentProperty");
      assertThat(result).isEqualTo(ItemCountValidationHandler.MSG_KEY_MAX_ITEMS_EXCEEDED);
    }

    @Test
    void resolveMessageKeyUsesSpecificKeyWhenPresent() {
      // The messages.properties bundle contains CompositionMaxItemsExceeded_RootTable_attachments
      String result =
          ItemCountValidationHandler.resolveMessageKey(
              ItemCountValidationHandler.MSG_KEY_MAX_ITEMS_EXCEEDED, "RootTable", "attachments");
      assertThat(result)
          .isEqualTo(
              ItemCountValidationHandler.MSG_KEY_MAX_ITEMS_EXCEEDED + "_RootTable_attachments");
    }
  }

  // ============================
  // Static helper method tests
  // ============================

  @Nested
  class StaticHelperTests {

    @Test
    @SuppressWarnings("unchecked")
    void getAnnotationIntValue_returnsIntegerForNumberValue() {
      CdsElement element = mock(CdsElement.class);
      CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
      when(annotation.getValue()).thenReturn(5);
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.of(annotation));

      var result =
          ItemCountValidationHandler.getAnnotationIntValue(
              element, ItemCountValidationHandler.ANNOTATION_MAX_ITEMS);

      assertThat(result).isPresent().contains(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAnnotationIntValue_returnsIntegerForStringValue() {
      CdsElement element = mock(CdsElement.class);
      CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
      when(annotation.getValue()).thenReturn("10");
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.of(annotation));

      var result =
          ItemCountValidationHandler.getAnnotationIntValue(
              element, ItemCountValidationHandler.ANNOTATION_MAX_ITEMS);

      assertThat(result).isPresent().contains(10);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAnnotationIntValue_returnsEmptyForNonNumericString() {
      CdsElement element = mock(CdsElement.class);
      CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
      when(annotation.getValue()).thenReturn("not-a-number");
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.of(annotation));

      var result =
          ItemCountValidationHandler.getAnnotationIntValue(
              element, ItemCountValidationHandler.ANNOTATION_MAX_ITEMS);

      assertThat(result).isEmpty();
    }

    @Test
    void getAnnotationIntValue_returnsEmptyForMissingAnnotation() {
      CdsElement element = mock(CdsElement.class);
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.empty());

      var result =
          ItemCountValidationHandler.getAnnotationIntValue(
              element, ItemCountValidationHandler.ANNOTATION_MAX_ITEMS);

      assertThat(result).isEmpty();
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

      assertThat(serviceAnnotation.type()).containsOnly(ApplicationService.class);
      assertThat(serviceAnnotation.value()).containsOnly("*");
    }

    @Test
    void processBeforeCreateHasCorrectAnnotations() throws NoSuchMethodException {
      var method =
          cut.getClass()
              .getDeclaredMethod("processBeforeCreate", CdsCreateEventContext.class, List.class);

      var beforeAnnotation = method.getAnnotation(Before.class);
      var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

      assertThat(beforeAnnotation).isNotNull();
      assertThat(beforeAnnotation.event()).containsExactly(CqnService.EVENT_CREATE);
      assertThat(handlerOrderAnnotation).isNotNull();
      assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
    }

    @Test
    void processBeforeUpdateHasCorrectAnnotations() throws NoSuchMethodException {
      var method =
          cut.getClass()
              .getDeclaredMethod("processBeforeUpdate", CdsUpdateEventContext.class, List.class);

      var beforeAnnotation = method.getAnnotation(Before.class);
      var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

      assertThat(beforeAnnotation).isNotNull();
      assertThat(beforeAnnotation.event()).containsExactly(CqnService.EVENT_UPDATE);
      assertThat(handlerOrderAnnotation).isNotNull();
      assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
    }
  }

  // ============================
  // Multiple data entries tests
  // ============================

  @Nested
  class MultipleDataEntriesTests {

    @Test
    void multipleRootsInPayload_eachValidatedIndependently() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, null);
      mockEntityElements(compositionEl);

      var root1 = createRootWithCompositionItems("attachments", 2); // within limit
      var root2 = createRootWithCompositionItems("attachments", 5); // exceeds limit

      cut.processBeforeCreate(createContext, List.of(root1, root2));

      // At least one error should be produced for root2
      verify(messages).error(anyString(), eq(3), eq("attachments"));
    }

    @Test
    void emptyDataList_noValidation() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 3, 1);
      mockEntityElements(compositionEl);

      cut.processBeforeCreate(createContext, Collections.emptyList());

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }
  }

  // ============================
  // validateCompositionItemCounts static method tests
  // ============================

  @Nested
  class ValidateCompositionItemCountsTests {

    @Test
    void validateStaticMethod_maxExceeded_producesError() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.ERROR);

      verify(messages).error(anyString(), eq(2), eq("attachments"));
    }

    @Test
    void validateStaticMethod_withinRange_noMessage() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 10, 1);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.ERROR);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void validateStaticMethod_warningSeverity_producesWarning() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.WARNING);

      verify(messages).warn(anyString(), eq(2), eq("attachments"));
      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void validateStaticMethod_infoSeverity_producesInfo() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      var root = createRootWithCompositionItems("attachments", 5);

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.INFO);

      verify(messages).info(anyString(), eq(2), eq("attachments"));
      verify(messages, never()).error(anyString(), anyInt(), anyString());
      verify(messages, never()).warn(anyString(), anyInt(), anyString());
    }

    @Test
    void validateStaticMethod_compositionValueNotList_skipsValidation() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);
      var root = CdsData.create();
      root.put("attachments", "not-a-list");

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.ERROR);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void validateStaticMethod_nonAssociationElement_skipsValidation() {
      CdsElement element = mock(CdsElement.class);
      CdsType type = mock(CdsType.class);
      when(type.isAssociation()).thenReturn(false);
      when(element.getType()).thenReturn(type);
      when(element.getName()).thenReturn("title");
      mockEntityElements(element);

      var root = CdsData.create();
      root.put("title", "some value");

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.ERROR);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void validateStaticMethod_associationNotComposition_skipsValidation() {
      CdsElement element = mock(CdsElement.class);
      CdsType type = mock(CdsType.class);
      when(type.isAssociation()).thenReturn(true);
      CdsAssociationType assocType = mock(CdsAssociationType.class);
      when(assocType.isComposition()).thenReturn(false);
      when(type.as(CdsAssociationType.class)).thenReturn(assocType);
      when(element.getType()).thenReturn(type);
      when(element.getName()).thenReturn("items");
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MAX_ITEMS))
          .thenReturn(Optional.empty());
      when(element.findAnnotation(ItemCountValidationHandler.ANNOTATION_MIN_ITEMS))
          .thenReturn(Optional.empty());
      mockEntityElements(element);

      var root = CdsData.create();
      root.put("items", createItems(5));

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, List.of(root), null, messages, Message.Severity.ERROR);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }

    @Test
    void validateStaticMethod_nullData_skipsValidation() {
      var compositionEl = createAnnotatedCompositionElement("attachments", 2, null);
      mockEntityElements(compositionEl);

      ItemCountValidationHandler.validateCompositionItemCounts(
          entity, null, null, messages, Message.Severity.ERROR);

      verify(messages, never()).error(anyString(), anyInt(), anyString());
    }
  }
}

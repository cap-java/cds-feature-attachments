/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsType;
import com.sap.cds.services.ServiceException;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ItemsCountValidationHandlerTest {

  private ItemsCountValidationHandler cut;
  private ThreadDataStorageReader storageReader;
  private CdsCreateEventContext createContext;
  private CdsUpdateEventContext updateContext;
  private Messages messages;
  private Message message;

  @BeforeEach
  void setup() {
    storageReader = mock(ThreadDataStorageReader.class);
    cut = new ItemsCountValidationHandler(storageReader);

    createContext = mock(CdsCreateEventContext.class);
    updateContext = mock(CdsUpdateEventContext.class);
    messages = mock(Messages.class);
    message = mock(Message.class);

    when(createContext.getMessages()).thenReturn(messages);
    when(updateContext.getMessages()).thenReturn(messages);
    when(messages.error(anyString(), any(Object[].class))).thenReturn(message);
    when(messages.warn(anyString(), any(Object[].class))).thenReturn(message);
    when(message.target(anyString(), any())).thenReturn(message);
  }

  @Test
  void classHasCorrectAnnotation() {
    var annotation = cut.getClass().getAnnotation(ServiceName.class);

    assertThat(annotation.type()).containsOnly(ApplicationService.class);
    assertThat(annotation.value()).containsOnly("*");
  }

  @Test
  void createMethodHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("validateOnCreate", CdsCreateEventContext.class, List.class);

    var beforeAnnotation = method.getAnnotation(Before.class);
    var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(beforeAnnotation.event()).containsExactly(CqnService.EVENT_CREATE);
    assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
  }

  @Test
  void updateMethodHasCorrectAnnotations() throws NoSuchMethodException {
    var method =
        cut.getClass()
            .getDeclaredMethod("validateOnUpdate", CdsUpdateEventContext.class, List.class);

    var beforeAnnotation = method.getAnnotation(Before.class);
    var handlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(beforeAnnotation.event()).containsExactly(CqnService.EVENT_UPDATE);
    assertThat(handlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
  }

  @Test
  void constructorRejectsNull() {
    assertThrows(NullPointerException.class, () -> new ItemsCountValidationHandler(null));
  }

  @Nested
  class MaxItemsValidation {

    @Test
    void maxItemsViolationAddsError() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 5, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      List<CdsData> items = createItems(6);
      root.put("attachments", items);

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq("attachments"), eq(5), eq(6));
      verify(message).target(eq("in"), any());
    }

    @Test
    void maxItemsNotViolatedNoMessage() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 5, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      List<CdsData> items = createItems(3);
      root.put("attachments", items);

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
      verify(messages, never()).warn(anyString(), any(Object[].class));
    }

    @Test
    void maxItemsExactBoundaryNoViolation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 5, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      List<CdsData> items = createItems(5);
      root.put("attachments", items);

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
    }

    @Test
    void maxItemsViolationDuringDraftActivationAddsWarning() {
      when(storageReader.get()).thenReturn(true);
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 3, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(5));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).warn(anyString(), eq("attachments"), eq(3), eq(5));
      verify(messages, never()).error(anyString(), any(Object[].class));
    }
  }

  @Nested
  class MinItemsValidation {

    @Test
    void minItemsViolationAddsError() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", null, null, 2);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(1));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq("attachments"), eq(2), eq(1));
      verify(message).target(eq("in"), any());
    }

    @Test
    void minItemsNotViolatedNoMessage() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", null, null, 2);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(3));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
      verify(messages, never()).warn(anyString(), any(Object[].class));
    }

    @Test
    void minItemsExactBoundaryNoViolation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", null, null, 2);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(2));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
    }

    @Test
    void minItemsViolationDuringDraftActivationAddsWarning() {
      when(storageReader.get()).thenReturn(true);
      CdsEntity entity = mockEntityWithAnnotation("attachments", null, null, 3);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(1));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).warn(anyString(), eq("attachments"), eq(3), eq(1));
      verify(messages, never()).error(anyString(), any(Object[].class));
    }
  }

  @Nested
  class CombinedValidation {

    @Test
    void bothMaxAndMinItemsViolatedAddsErrors() {
      // Max=5, Min=2, count=0 -> min violation
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 5, 2);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(0));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq("attachments"), eq(2), eq(0));
      verify(messages, never()).error(anyString(), eq("attachments"), eq(5), eq(0));
    }

    @Test
    void maxViolatedButNotMinAddsOnlyMaxError() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 3, 1);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(5));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).error(anyString(), eq("attachments"), eq(3), eq(5));
    }
  }

  @Nested
  class CompositionNotInPayload {

    @Test
    void compositionNotPresentInDataSkipsValidation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 2, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("title", "some title");
      // no "attachments" key in data

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
      verify(messages, never()).warn(anyString(), any(Object[].class));
    }

    @Test
    void nonListCompositionDataSkipsValidation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 2, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", "not a list");

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
    }
  }

  @Nested
  class NoAnnotations {

    @Test
    void entityWithoutAnnotationsSkipsValidation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", null, null, null);
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(100));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
      verify(messages, never()).warn(anyString(), any(Object[].class));
    }

    @Test
    void annotationWithTrueValueIsIgnored() {
      CdsEntity entity = mockEntityWithTrueAnnotation("attachments");
      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(100));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
    }
  }

  @Nested
  class UpdateEvent {

    @Test
    void updateWithMaxItemsViolation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 3, null);
      when(updateContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(5));

      cut.validateOnUpdate(updateContext, List.of(root));

      verify(messages).error(anyString(), eq("attachments"), eq(3), eq(5));
    }
  }

  @Nested
  class AnnotationValueResolution {

    @Test
    void integerAnnotationValue() {
      CdsData data = CdsData.create();
      int result = ItemsCountValidationHandler.resolveAnnotationValue(42, data);
      assertThat(result).isEqualTo(42);
    }

    @Test
    void stringIntegerAnnotationValue() {
      CdsData data = CdsData.create();
      int result = ItemsCountValidationHandler.resolveAnnotationValue("10", data);
      assertThat(result).isEqualTo(10);
    }

    @Test
    void propertyReferenceAnnotationValue() {
      CdsData data = CdsData.create();
      data.put("stock", 15);
      int result = ItemsCountValidationHandler.resolveAnnotationValue("stock", data);
      assertThat(result).isEqualTo(15);
    }

    @Test
    void propertyReferenceNotFoundReturnsNegative() {
      CdsData data = CdsData.create();
      int result = ItemsCountValidationHandler.resolveAnnotationValue("nonExistent", data);
      assertThat(result).isEqualTo(-1);
    }

    @Test
    void propertyReferenceNonNumericReturnsNegative() {
      CdsData data = CdsData.create();
      data.put("name", "text");
      int result = ItemsCountValidationHandler.resolveAnnotationValue("name", data);
      assertThat(result).isEqualTo(-1);
    }

    @Test
    void unknownTypeReturnsNegative() {
      CdsData data = CdsData.create();
      int result = ItemsCountValidationHandler.resolveAnnotationValue(new Object(), data);
      assertThat(result).isEqualTo(-1);
    }
  }

  @Nested
  class MessageKeyResolution {

    @Test
    void baseKeyUsedWhenSpecificNotDefined() {
      String key =
          ItemsCountValidationHandler.resolveMessageKey(
              "Validation_MaxItems", "my.Entity", "attachments");
      // When no specific key in bundle, falls back to base key
      assertThat(key).isEqualTo("Validation_MaxItems");
    }
  }

  @Nested
  class ThrowIfError {

    @Test
    void throwIfErrorCalledForActiveOperation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 2, null);
      when(createContext.getTarget()).thenReturn(entity);
      when(storageReader.get()).thenReturn(false);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(5));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages).throwIfError();
    }

    @Test
    void throwIfErrorNotCalledForDraftActivation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 2, null);
      when(createContext.getTarget()).thenReturn(entity);
      when(storageReader.get()).thenReturn(true);

      CdsData root = CdsData.create();
      root.put("attachments", createItems(5));

      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).throwIfError();
    }

    @Test
    void throwIfErrorCausesExceptionOnViolation() {
      CdsEntity entity = mockEntityWithAnnotation("attachments", "Validation.MaxItems", 2, null);
      when(createContext.getTarget()).thenReturn(entity);
      doThrow(new ServiceException("validation error")).when(messages).throwIfError();

      CdsData root = CdsData.create();
      root.put("attachments", createItems(5));

      assertThrows(
          ServiceException.class, () -> cut.validateOnCreate(createContext, List.of(root)));
    }
  }

  @Nested
  class NonCompositionElements {

    @Test
    void nonCompositionElementsAreSkipped() {
      CdsEntity entity = mock(CdsEntity.class);
      when(entity.getQualifiedName()).thenReturn("test.Entity");

      CdsElement element = mock(CdsElement.class);
      CdsType type = mock(CdsType.class);
      when(type.isAssociation()).thenReturn(false);
      when(element.getType()).thenReturn(type);
      when(entity.elements()).thenReturn(Stream.of(element));

      when(createContext.getTarget()).thenReturn(entity);

      CdsData root = CdsData.create();
      cut.validateOnCreate(createContext, List.of(root));

      verify(messages, never()).error(anyString(), any(Object[].class));
    }
  }

  // --- Helper methods ---

  private List<CdsData> createItems(int count) {
    List<CdsData> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      items.add(CdsData.create());
    }
    return items;
  }

  @SuppressWarnings("unchecked")
  private CdsEntity mockEntityWithAnnotation(
      String compositionName, String maxAnnotation, Integer maxValue, Integer minValue) {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn("test.Entity");

    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn(compositionName);

    CdsType type = mock(CdsType.class);
    when(type.isAssociation()).thenReturn(true);

    CdsAssociationType assocType = mock(CdsAssociationType.class);
    when(assocType.isComposition()).thenReturn(true);
    when(type.as(CdsAssociationType.class)).thenReturn(assocType);
    when(element.getType()).thenReturn(type);

    if (maxValue != null) {
      CdsAnnotation<Object> maxAnnot = mock(CdsAnnotation.class);
      when(maxAnnot.getValue()).thenReturn(maxValue);
      when(element.findAnnotation("Validation.MaxItems")).thenReturn(Optional.of(maxAnnot));
    } else {
      when(element.findAnnotation("Validation.MaxItems")).thenReturn(Optional.empty());
    }

    if (minValue != null) {
      CdsAnnotation<Object> minAnnot = mock(CdsAnnotation.class);
      when(minAnnot.getValue()).thenReturn(minValue);
      when(element.findAnnotation("Validation.MinItems")).thenReturn(Optional.of(minAnnot));
    } else {
      when(element.findAnnotation("Validation.MinItems")).thenReturn(Optional.empty());
    }

    when(entity.elements()).thenReturn(Stream.of(element));
    return entity;
  }

  @SuppressWarnings("unchecked")
  private CdsEntity mockEntityWithTrueAnnotation(String compositionName) {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn("test.Entity");

    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn(compositionName);

    CdsType type = mock(CdsType.class);
    when(type.isAssociation()).thenReturn(true);
    CdsAssociationType assocType = mock(CdsAssociationType.class);
    when(assocType.isComposition()).thenReturn(true);
    when(type.as(CdsAssociationType.class)).thenReturn(assocType);
    when(element.getType()).thenReturn(type);

    CdsAnnotation<Object> maxAnnot = mock(CdsAnnotation.class);
    when(maxAnnot.getValue()).thenReturn("true");
    when(element.findAnnotation("Validation.MaxItems")).thenReturn(Optional.of(maxAnnot));
    when(element.findAnnotation("Validation.MinItems")).thenReturn(Optional.empty());

    when(entity.elements()).thenReturn(Stream.of(element));
    return entity;
  }
}

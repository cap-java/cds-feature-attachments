/*
 * © 2024-2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.AttachmentValidationHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class ApplicationHandlerHelperTest {

  @Mock private CdsEntity entity;
  @Mock private CdsData data;
  @Mock private CdsRuntime cdsRuntime;
  @Mock private CdsModel cdsModel;
  @Mock private CdsEntity serviceEntity;
  @Mock private CdsElement cdsElement;
  @Mock private CdsAnnotation<Object> annotation;
  @Mock private CdsDataProcessor processor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(cdsRuntime.getCdsModel()).thenReturn(cdsModel);
  }

  @Test
  void keysAreInData() {
    Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
    var data = CdsData.create();
    data.put("key1", "value1");
    data.put("key2", "value2");
    data.put("data", "value3");
    var result = ApplicationHandlerHelper.areKeysInData(keys, data);

    assertThat(result).isTrue();
  }

  @Test
  void keyMissingInData() {
    Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
    var data = CdsData.create();
    data.put("key1", "value1");
    data.put("data", "value3");
    var result = ApplicationHandlerHelper.areKeysInData(keys, data);

    assertThat(result).isFalse();
  }

  @Test
  void keyHasWrongValue() {
    Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
    var data = CdsData.create();
    data.put("key1", "value1");
    data.put("key2", "wrong value");
    data.put("data", "value3");
    var result = ApplicationHandlerHelper.areKeysInData(keys, data);

    assertThat(result).isFalse();
  }

  @Test
  void removeDraftKey() {
    Map<String, Object> keys = Map.of("key1", "value1", "IsActiveEntity", "true");
    assertTrue(keys.containsKey("IsActiveEntity"));

    Map<String, Object> result = ApplicationHandlerHelper.removeDraftKey(keys);
    assertFalse(result.containsKey("IsActiveEntity"));
    assertTrue(result.containsKey("key1"));
  }

  @Test
  void validateAcceptableMediaTypes_shouldReturnWhenEntityIsNull() {
    // given
    CdsRuntime runtime = mock(CdsRuntime.class);

    // when / then
    assertDoesNotThrow(
        () -> ApplicationHandlerHelper.validateAcceptableMediaTypes(null, List.of(), runtime));

    // ensure no further interaction happens
    verifyNoInteractions(runtime);
  }

  @Test
  void getEntityAcceptableMediaTypes_returnsAnnotationValue() {
    List<String> expectedTypes = List.of("image/png", "application/pdf");

    when(entity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(expectedTypes);
    assertEquals(expectedTypes, ApplicationHandlerHelper.getEntityAcceptableMediaTypes(entity));
  }

  @Test
  void getEntityAcceptableMediaTypes_missingAnnotation_returnsWildcard() {
    when(entity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes")).thenReturn(Optional.empty());
    assertEquals(List.of("*/*"), ApplicationHandlerHelper.getEntityAcceptableMediaTypes(entity));
  }

  @Test
  void getEntityAcceptableMediaTypes_nullAnnotationValue_returnsWildcard() {
    when(entity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(null);
    assertEquals(List.of("*/*"), ApplicationHandlerHelper.getEntityAcceptableMediaTypes(entity));
  }

  @Test
  void extractFileName_whenFileNamePresent_returnsValue() {
    when(cdsElement.getName()).thenReturn("fileName");
    try (MockedStatic<CdsDataProcessor> mocked = mockStatic(CdsDataProcessor.class)) {
      mocked.when(CdsDataProcessor::create).thenReturn(processor);
      doAnswer(
              invocation -> {
                Filter filter = invocation.getArgument(0);
                Validator validator = invocation.getArgument(1);

                // Simulate processor visiting fileName with String value
                if (filter.test(null, cdsElement, null)) {
                  validator.validate(null, cdsElement, "test.pdf");
                }
                return processor;
              })
          .when(processor)
          .addValidator(any(), any());

      doNothing().when(processor).process(anyList(), any());
      String result = ApplicationHandlerHelper.extractFileName(entity, List.of(data));
      assertThat(result).isEqualTo("test.pdf");
    }
  }

  @Test
  void extractFileName_whenElementIsNotFileName_throws() {
    when(cdsElement.getName()).thenReturn("content");
    try (MockedStatic<CdsDataProcessor> mocked = mockStatic(CdsDataProcessor.class)) {
      mocked.when(CdsDataProcessor::create).thenReturn(processor);
      doAnswer(
              invocation -> {
                Filter filter = invocation.getArgument(0);
                Validator validator = invocation.getArgument(1);
                if (filter.test(null, cdsElement, null)) {
                  validator.validate(null, cdsElement, "test.pdf");
                }
                return processor;
              })
          .when(processor)
          .addValidator(any(), any());

      doNothing().when(processor).process(anyList(), any());
      assertThrows(
          ServiceException.class,
          () -> ApplicationHandlerHelper.extractFileName(entity, List.of(data)));
    }
  }

  @Test
  void extractFileName_valueIsNotString_branchCovered() {
    when(cdsElement.getName()).thenReturn("fileName");
    try (MockedStatic<CdsDataProcessor> mocked = mockStatic(CdsDataProcessor.class)) {
      mocked.when(CdsDataProcessor::create).thenReturn(processor);
      doAnswer(
              invocation -> {
                Filter filter = invocation.getArgument(0);
                Validator validator = invocation.getArgument(1);
                if (filter.test(null, cdsElement, null)) {
                  validator.validate(null, cdsElement, 42); // non-String
                }
                return processor;
              })
          .when(processor)
          .addValidator(any(), any());
      doNothing().when(processor).process(anyList(), any());

      assertThrows(
          ServiceException.class,
          () -> ApplicationHandlerHelper.extractFileName(entity, List.of(data)));
    }
  }

  @Test
  void validateAcceptableMediaTypes_nonMediaOrMissingEntity_doesNothing() {
    when(entity.getQualifiedName()).thenReturn("TestService.Roots");
    when(cdsModel.findEntity("TestService.Roots")).thenReturn(Optional.empty());

    assertDoesNotThrow(
        () -> ApplicationHandlerHelper.validateAcceptableMediaTypes(entity, List.of(), cdsRuntime));
  }

  @Test
  void validateAcceptableMediaTypes_notMediaEntity_doesNotCallValidation() {
    when(entity.getQualifiedName()).thenReturn("TestService.Roots");
    when(cdsModel.findEntity("TestService.Roots")).thenReturn(Optional.of(serviceEntity));
    when(serviceEntity.getAnnotationValue("_is_media_data", false)).thenReturn(false);

    try (MockedStatic<AttachmentValidationHelper> mocked =
        mockStatic(AttachmentValidationHelper.class)) {
      ApplicationHandlerHelper.validateAcceptableMediaTypes(entity, List.of(), cdsRuntime);
      mocked.verifyNoInteractions();
    }
  }

  @Test
  void shouldNotThrowWhenEntityNotFoundInModel() {
    when(entity.getQualifiedName()).thenReturn("Test.Entity");
    when(cdsModel.findEntity("Test.Entity")).thenReturn(Optional.empty());

    assertDoesNotThrow(
        () -> ApplicationHandlerHelper.validateAcceptableMediaTypes(entity, List.of(), cdsRuntime));
  }

  @Test
  void validateAcceptableMediaTypes_mediaTypeMatches_succeeds() {
    setupMediaEntity(List.of("image/png"));
    try (MockedStatic<ApplicationHandlerHelper> helperStatic =
        mockStatic(ApplicationHandlerHelper.class, CALLS_REAL_METHODS)) {

      // file.png → media type image/png → allowed
      helperStatic
          .when(() -> ApplicationHandlerHelper.extractFileName(entity, List.of(data)))
          .thenReturn("file.png");

      assertDoesNotThrow(
          () ->
              ApplicationHandlerHelper.validateAcceptableMediaTypes(
                  entity, List.of(data), cdsRuntime));
    }
  }

  @Test
  void validateAcceptableMediaTypes_mediaTypeMismatch_throws() {
    setupMediaEntity(List.of("image/png"));
    try (MockedStatic<ApplicationHandlerHelper> helperStatic =
        mockStatic(ApplicationHandlerHelper.class, CALLS_REAL_METHODS)) {

      // file.jpg → media type image/jpeg → NOT allowed
      helperStatic
          .when(() -> ApplicationHandlerHelper.extractFileName(entity, List.of(data)))
          .thenReturn("file.jpg");

      assertThrows(
          ServiceException.class,
          () ->
              ApplicationHandlerHelper.validateAcceptableMediaTypes(
                  entity, List.of(data), cdsRuntime));
    }
  }

  private void setupMediaEntity(List<String> allowedTypes) {
    when(entity.getQualifiedName()).thenReturn("Test.Entity");
    when(cdsModel.findEntity("Test.Entity")).thenReturn(Optional.of(serviceEntity));
    when(serviceEntity.getAnnotationValue("_is_media_data", false)).thenReturn(true);
    when(serviceEntity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(allowedTypes);
  }
}

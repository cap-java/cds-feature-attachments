/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.AttachmentValidationHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ServiceException;
import com.sap.cds.reflect.CdsAnnotation;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.sap.cds.reflect.CdsElement;

class ApplicationHandlerHelperTest {

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

  // ----------- Test validateAcceptableMediaTypes -----------

  @Test
  void extractFileName_whenFileNamePresent_returnsValue() {

    CdsEntity entity = mock(CdsEntity.class);
    CdsData data = mock(CdsData.class);
    CdsElement fileNameElement = mock(CdsElement.class);
    when(fileNameElement.getName()).thenReturn("fileName");
    CdsDataProcessor processor = mock(CdsDataProcessor.class);

    try (MockedStatic<CdsDataProcessor> mocked = mockStatic(CdsDataProcessor.class)) {
      mocked.when(CdsDataProcessor::create).thenReturn(processor);
      doAnswer(invocation -> {
        Filter filter = invocation.getArgument(0);
        Validator validator = invocation.getArgument(1);

        // Simulate processor visiting fileName with String value
        if (filter.test(null, fileNameElement, null)) {
          validator.validate(null, fileNameElement, "test.pdf");
        }
        return processor;
      }).when(processor).addValidator(any(), any());

      doNothing().when(processor).process(anyList(), any());
      String result = ApplicationHandlerHelper.extractFileName(entity, List.of(data));
      assertThat(result).isEqualTo("test.pdf");
    }
  }

  @Test
  void extractFileName_whenElementIsNotFileName_throws() {

    CdsEntity entity = mock(CdsEntity.class);
    CdsData data = mock(CdsData.class);
    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn("content");
    CdsDataProcessor processor = mock(CdsDataProcessor.class);

    try (MockedStatic<CdsDataProcessor> mocked = mockStatic(CdsDataProcessor.class)) {
      mocked.when(CdsDataProcessor::create).thenReturn(processor);
      doAnswer(invocation -> {
        Filter filter = invocation.getArgument(0);
        Validator validator = invocation.getArgument(1);
        if (filter.test(null, element, null)) {
          validator.validate(null, element, "test.pdf");
        }
        return processor;
      }).when(processor).addValidator(any(), any());

      doNothing().when(processor).process(anyList(), any());
      assertThrows(ServiceException.class, () -> ApplicationHandlerHelper.extractFileName(entity, List.of(data)));
    }
  }

  @Test
  void extractFileName_valueIsNotString_branchCovered() {

    CdsEntity entity = mock(CdsEntity.class);
    CdsData data = mock(CdsData.class);
    CdsElement element = mock(CdsElement.class);
    when(element.getName()).thenReturn("fileName");
    CdsDataProcessor processor = mock(CdsDataProcessor.class);

    try (MockedStatic<CdsDataProcessor> mocked = mockStatic(CdsDataProcessor.class)) {
      mocked.when(CdsDataProcessor::create).thenReturn(processor);
      doAnswer(invocation -> {
        Filter filter = invocation.getArgument(0);
        Validator validator = invocation.getArgument(1);
        if (filter.test(null, element, null)) {
          validator.validate(null, element, 42); // non-String
        }
        return processor;
      }).when(processor).addValidator(any(), any());
      doNothing().when(processor).process(anyList(), any());
      assertThrows(ServiceException.class, () -> ApplicationHandlerHelper.extractFileName(entity, List.of(data)));
    }
  }

  // ----------- Test getEntityAcceptableMediaTypes -----------
  @Test
  void getEntityAcceptableMediaTypes_withAnnotation() {
    CdsEntity entity = mock(CdsEntity.class);
    CdsElement content = mock(CdsElement.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(entity.getElement("content")).thenReturn(content);
    when(content.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue())
        .thenReturn(List.of("image/png", "image/jpeg"));
    List<String> result = ApplicationHandlerHelper.getEntityAcceptableMediaTypes(entity);
    assertThat(result).containsExactly("image/png", "image/jpeg");
  }

  @Test
  void getEntityAcceptableMediaTypes_withoutAnnotation_returnsWildcard() {
    CdsEntity entity = mock(CdsEntity.class);
    CdsElement content = mock(CdsElement.class);
    when(entity.getElement("content")).thenReturn(content);
    when(content.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.empty());
    List<String> result = ApplicationHandlerHelper.getEntityAcceptableMediaTypes(entity);
    assertThat(result).containsExactly("*/*");
  }

  @Test
  void validateAcceptableMediaTypes_success_executesAllLines() {
    CdsEntity entity = mock(CdsEntity.class);
    CdsElement content = mock(CdsElement.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(entity.getElement("content")).thenReturn(content);
    when(content.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue())
        .thenReturn(List.of("image/png"));
    List<CdsData> data = List.of(mock(CdsData.class));
    try (MockedStatic<ApplicationHandlerHelper> helper = mockStatic(ApplicationHandlerHelper.class, CALLS_REAL_METHODS);
        MockedStatic<AttachmentValidationHelper> validator = mockStatic(AttachmentValidationHelper.class)) {
      // allow real getEntityAcceptableMediaTypes
      helper.when(() -> ApplicationHandlerHelper.extractFileName(entity, data))
          .thenReturn("test.png");
      assertDoesNotThrow(() -> ApplicationHandlerHelper.validateAcceptableMediaTypes(entity, data));
      validator.verify(() -> AttachmentValidationHelper.validateMediaTypeForAttachment(
          "test.png",
          List.of("image/png")));
    }
  }

}

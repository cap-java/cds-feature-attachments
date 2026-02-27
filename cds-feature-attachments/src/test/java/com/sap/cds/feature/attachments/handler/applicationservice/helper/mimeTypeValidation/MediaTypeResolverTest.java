/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class MediaTypeResolverTest {

  @AfterEach
  void reset() {
    MediaTypeResolver.setCascader(new AssociationCascader());
  }

  @Test
  void shouldReturnEmptyMapWhenNoMediaEntitiesFound() {
    AssociationCascader mockCascader = mock(AssociationCascader.class);
    MediaTypeResolver.setCascader(mockCascader);

    CdsModel model = mock(CdsModel.class);
    CdsEntity root = mock(CdsEntity.class);

    when(mockCascader.findMediaEntityNames(model, root)).thenReturn(List.of());

    Map<String, List<String>> result =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(root, model);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnMediaTypesFromAnnotation() {
    CdsModel model = mock(CdsModel.class);
    CdsEntity root = mock(CdsEntity.class);
    CdsEntity media = mock(CdsEntity.class);
    CdsElement element = mock(CdsElement.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);

    AssociationCascader cascader = mock(AssociationCascader.class);
    MediaTypeResolver.setCascader(cascader);

    when(cascader.findMediaEntityNames(model, root)).thenReturn(List.of("MediaEntity"));
    when(model.getEntity("MediaEntity")).thenReturn(media);

    when(media.getElement("content")).thenReturn(element);
    when(element.findAnnotation("Core.AcceptableMediaTypes")).thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(List.of("image/png", "image/jpeg"));

    Map<String, List<String>> result =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(root, model);

    assertThat(result.get("MediaEntity")).containsExactly("image/png", "image/jpeg");
  }

  @Test
  void shouldResolveMediaTypesUsingCascader() {
    try (MockedStatic<ApplicationHandlerHelper> mocked =
        mockStatic(ApplicationHandlerHelper.class)) {

      // Arrange
      CdsModel model = mock(CdsModel.class);
      CdsEntity root = mock(CdsEntity.class);
      CdsEntity media = mock(CdsEntity.class);
      AssociationCascader mockCascader = mock(AssociationCascader.class);
      MediaTypeResolver.setCascader(mockCascader);

      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(any())).thenReturn(false);
      when(mockCascader.findMediaEntityNames(model, root)).thenReturn(List.of("MediaEntity"));
      when(model.getEntity("MediaEntity")).thenReturn(media);
      when(media.getElement(any())).thenReturn(null);

      // Act
      Map<String, List<String>> result =
          MediaTypeResolver.getAcceptableMediaTypesFromEntity(root, model);

      // Assert
      assertThat(result).containsKey("MediaEntity");
    }
  }
}

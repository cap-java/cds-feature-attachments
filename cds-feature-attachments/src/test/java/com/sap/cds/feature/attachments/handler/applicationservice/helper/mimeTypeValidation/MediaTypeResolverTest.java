/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MediaTypeResolverTest {

  @Test
  void shouldReturnEmptyMapWhenNoMediaEntitiesFound() {
    CdsModel model = mock(CdsModel.class);
    Map<String, List<String>> result =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(model, List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnMediaTypesFromAnnotation() {
    CdsModel model = mock(CdsModel.class);
    CdsEntity media = mock(CdsEntity.class);
    CdsElement element = mock(CdsElement.class);
    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);

    when(model.getEntity("MediaEntity")).thenReturn(media);

    when(media.getElement("content")).thenReturn(element);
    when(element.findAnnotation("Core.AcceptableMediaTypes")).thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(List.of("image/png", "image/jpeg"));

    Map<String, List<String>> result =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(model, List.of("MediaEntity"));

    assertThat(result.get("MediaEntity")).containsExactly("image/png", "image/jpeg");
  }

  @Test
  void shouldResolveMediaTypesUsingCascader() {
    CdsModel model = mock(CdsModel.class);
    CdsEntity media = mock(CdsEntity.class);

    when(model.getEntity("MediaEntity")).thenReturn(media);
    when(media.getElement(any())).thenReturn(null);

    Map<String, List<String>> result =
        MediaTypeResolver.getAcceptableMediaTypesFromEntity(model, List.of("MediaEntity"));

    assertThat(result).containsKey("MediaEntity");
  }
}

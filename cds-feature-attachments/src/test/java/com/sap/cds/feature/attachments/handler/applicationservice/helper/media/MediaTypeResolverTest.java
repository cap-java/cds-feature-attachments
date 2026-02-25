/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.media;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class MediaTypeResolverTest {

  @Test
  void returnsMediaTypes_whenEntityIsMediaEntity() {
    CdsEntity entity = mediaEntity("MediaEntity", List.of("image/png", "image/jpeg"));
    try (MockedStatic<ApplicationHandlerHelper> mocked = mockMedia(entity)) {
      Map<String, List<String>> result = MediaTypeResolver.getAcceptableMediaTypesFromEntity(entity);
      assertEquals(Map.of("MediaEntity", List.of("image/png", "image/jpeg")), result);
    }
  }

  @Test
  void returnsWildcard_whenAnnotationMissing() {
    CdsEntity entity = mediaEntity("MediaEntity", null);
    try (MockedStatic<ApplicationHandlerHelper> mocked = mockMedia(entity)) {
      Map<String, List<String>> result = MediaTypeResolver.getAcceptableMediaTypesFromEntity(entity);
      assertEquals(List.of("*/*"), result.get("MediaEntity"));
    }
  }

  @Test
  void returnsMediaTypes_fromComposedChildEntities() {
    CdsEntity child = mediaEntity("ChildMediaEntity", List.of("application/pdf"));
    CdsEntity root = rootWithChild(child);
    try (MockedStatic<ApplicationHandlerHelper> mocked = mockMedia(child)) {
      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(root)).thenReturn(false);
      Map<String, List<String>> result = MediaTypeResolver.getAcceptableMediaTypesFromEntity(root);
      assertEquals(Map.of("ChildMediaEntity", List.of("application/pdf")), result);
    }
  }

  @Test
  void ignoresNonMediaChildren() {
    CdsEntity child = mock(CdsEntity.class);
    CdsEntity root = rootWithChild(child);
    try (MockedStatic<ApplicationHandlerHelper> mocked = mockStatic(ApplicationHandlerHelper.class)) {
      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(root)).thenReturn(false);
      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(child)).thenReturn(false);
      Map<String, List<String>> result = MediaTypeResolver.getAcceptableMediaTypesFromEntity(root);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void returnsEmpty_whenNoAssociations() {
    CdsEntity root = mock(CdsEntity.class);
    when(root.elements()).thenReturn(Stream.empty());
    try (MockedStatic<ApplicationHandlerHelper> mocked = mockStatic(ApplicationHandlerHelper.class)) {
      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(root)).thenReturn(false);
      Map<String, List<String>> result = MediaTypeResolver.getAcceptableMediaTypesFromEntity(root);
      assertTrue(result.isEmpty());
    }
  }

  // ----------- HELPERS ----------
  private CdsEntity mediaEntity(String name, List<String> mediaTypes) {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn(name);
    if (mediaTypes != null) {
      CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
      when(annotation.getValue()).thenReturn(mediaTypes);
      CdsElement content = mock(CdsElement.class);
      when(content.findAnnotation("Core.AcceptableMediaTypes")).thenReturn(Optional.of(annotation));
      when(entity.getElement("content")).thenReturn(content);
    } else {
      when(entity.getElement("content")).thenReturn(null);
    }
    return entity;
  }

  private CdsEntity rootWithChild(CdsEntity child) {
    CdsAssociationType association = mock(CdsAssociationType.class);
    when(association.isComposition()).thenReturn(true);
    when(association.getTarget()).thenReturn(child);

    CdsType type = mock(CdsType.class);
    when(type.isAssociation()).thenReturn(true);
    when(type.as(CdsAssociationType.class)).thenReturn(association);

    CdsElement element = mock(CdsElement.class);
    when(element.getType()).thenReturn(type);

    CdsEntity root = mock(CdsEntity.class);
    when(root.elements()).thenReturn(Stream.of(element));

    return root;
  }

  private MockedStatic<ApplicationHandlerHelper> mockMedia(CdsEntity mediaEntity) {
    MockedStatic<ApplicationHandlerHelper> mocked = mockStatic(ApplicationHandlerHelper.class);
    mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(mediaEntity)).thenReturn(true);
    return mocked;
  }
}

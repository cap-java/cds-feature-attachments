/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.AttachmentDataExtractor;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.media.MediaTypeResolver;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AttachmentValidationHelperTest {

  @Test
  void returns_whenEntityIsNull() {
    assertDoesNotThrow(
        () ->
            AttachmentValidationHelper.validateMediaAttachments(
                null, List.of(), mock(CdsRuntime.class)));
  }

  @Test
  void returns_whenEntityNotFoundInModel() {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn("Entity");

    CdsModel model = mock(CdsModel.class);
    when(model.findEntity("Entity")).thenReturn(Optional.empty());

    CdsRuntime runtime = mockRuntime(model);

    assertDoesNotThrow(
        () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));
  }

  @Test
  void returns_whenNotMediaEntityAndNoAttachments() {
    CdsEntity entity = mockEntity("Entity");

    try (MockedStatic<ApplicationHandlerHelper> mocked =
        mockStatic(ApplicationHandlerHelper.class)) {
      mocked
          .when(() -> ApplicationHandlerHelper.deepSearchForAttachments(entity))
          .thenReturn(false);
      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(false);

      CdsRuntime runtime = mockRuntime(entity);

      assertDoesNotThrow(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));
    }
  }

  @Test
  void passes_whenAllFilesValid() {
    CdsEntity entity = mockEntity("Entity");

    Map<String, List<String>> allowed = Map.of("Entity", List.of("image/png"));
    Map<String, Set<String>> files = Map.of("Entity", Set.of("file.png"));

    try (MockedStatic<ApplicationHandlerHelper> helper =
            mockStatic(ApplicationHandlerHelper.class);
        MockedStatic<MediaTypeResolver> resolver = mockStatic(MediaTypeResolver.class);
        MockedStatic<AttachmentDataExtractor> extractor =
            mockStatic(AttachmentDataExtractor.class)) {

      helper.when(() -> ApplicationHandlerHelper.deepSearchForAttachments(entity)).thenReturn(true);
      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(true);

      resolver
          .when(() -> MediaTypeResolver.getAcceptableMediaTypesFromEntity(entity))
          .thenReturn(allowed);

      extractor
          .when(() -> AttachmentDataExtractor.extractFileNamesByElement(entity, List.of()))
          .thenReturn(files);

      CdsRuntime runtime = mockRuntime(entity);

      assertDoesNotThrow(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));
    }
  }

  @Test
  void throwsException_whenInvalidFilesDetected() {
    CdsEntity entity = mockEntity("Entity");

    Map<String, List<String>> allowed = Map.of("Entity", List.of("image/png"));
    Map<String, Set<String>> files = Map.of("Entity", Set.of("file.txt"));

    try (MockedStatic<ApplicationHandlerHelper> helper =
            mockStatic(ApplicationHandlerHelper.class);
        MockedStatic<MediaTypeResolver> resolver = mockStatic(MediaTypeResolver.class);
        MockedStatic<AttachmentDataExtractor> extractor =
            mockStatic(AttachmentDataExtractor.class)) {

      helper.when(() -> ApplicationHandlerHelper.deepSearchForAttachments(entity)).thenReturn(true);
      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(true);

      resolver
          .when(() -> MediaTypeResolver.getAcceptableMediaTypesFromEntity(entity))
          .thenReturn(allowed);

      extractor
          .when(() -> AttachmentDataExtractor.extractFileNamesByElement(entity, List.of()))
          .thenReturn(files);

      CdsRuntime runtime = mockRuntime(entity);

      ServiceException ex =
          assertThrows(
              ServiceException.class,
              () ->
                  AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));

      assertTrue(ex.getMessage().contains("Unsupported file types detected"));
      assertTrue(ex.getMessage().contains("file.txt"));
      assertTrue(ex.getMessage().contains("image/png"));
    }
  }

  @Test
  void groupsInvalidFilesByElement_andBuildsReadableMessage() {
    Map<String, Set<String>> files = Map.of("com.test.Entity", Set.of("file.txt", "file.pdf"));

    Map<String, List<String>> allowed = Map.of("com.test.Entity", List.of("image/png"));

    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () -> AttachmentValidationHelper.validateAttachmentMediaTypes(files, allowed));

    assertTrue(ex.getMessage().contains("Entity")); // simple name extraction
    assertTrue(ex.getMessage().contains("file.txt"));
    assertTrue(ex.getMessage().contains("file.pdf"));
    assertTrue(ex.getMessage().contains("image/png"));
  }

  @Test
  void doesNothing_whenNoFilesProvided() {
    assertDoesNotThrow(
        () -> AttachmentValidationHelper.validateAttachmentMediaTypes(Map.of(), Map.of()));
  }

  @Test
  void usesWildcard_whenNoAllowedTypesDefined() {
    Map<String, Set<String>> files = Map.of("Entity", Set.of("file.anything"));

    assertDoesNotThrow(
        () -> AttachmentValidationHelper.validateAttachmentMediaTypes(files, Map.of()));
  }

  @Test
  void throwsException_whenFilenameInvalid() {
    Map<String, Set<String>> files = Map.of("Entity", Set.of("invalid")); // no extension

    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () -> AttachmentValidationHelper.validateAttachmentMediaTypes(files, Map.of()));

    assertTrue(ex.getMessage().contains("Invalid filename format"));
  }

  private CdsRuntime mockRuntime(CdsEntity entity) {
    CdsModel model = mock(CdsModel.class);
    when(model.findEntity(entity.getQualifiedName())).thenReturn(Optional.of(entity));

    CdsRuntime runtime = mock(CdsRuntime.class);
    when(runtime.getCdsModel()).thenReturn(model);

    return runtime;
  }

  private CdsRuntime mockRuntime(CdsModel model) {
    CdsRuntime runtime = mock(CdsRuntime.class);
    when(runtime.getCdsModel()).thenReturn(model);
    return runtime;
  }

  private CdsEntity mockEntity(String name) {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn(name);
    return entity;
  }
}

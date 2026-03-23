/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class AttachmentValidationHelperTest {

  private static final String ATTACHMENTS_ENTITY = "Entity.attachments";

  @Test
  void doesNothing_whenEntityIsNull() {
    assertDoesNotThrow(
        () -> AttachmentValidationHelper.validateMediaAttachments(null, List.of(), null));
  }

  @Test
  void doesNothing_whenEntityNotFoundInModel() {
    CdsEntity entity = mock(CdsEntity.class);
    when(entity.getQualifiedName()).thenReturn("Entity");

    CdsModel model = mock(CdsModel.class);
    when(model.findEntity("Entity")).thenReturn(Optional.empty());

    CdsRuntime runtime = mockRuntime(model);

    try (MockedStatic<ApplicationHandlerHelper> helper =
        mockStatic(ApplicationHandlerHelper.class)) {
      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(false);

      setupMockCascader(entity, model, false);

      assertDoesNotThrow(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));
    }
  }

  @Test
  void doesNothing_whenNoEntityHasAcceptableMediaTypesAnnotation() {
    CdsEntity entity = mockEntity("Entity");
    CdsRuntime runtime = mockRuntime(entity);

    try (MockedStatic<ApplicationHandlerHelper> helper =
            mockStatic(ApplicationHandlerHelper.class);
        MockedStatic<MediaTypeResolver> resolver = mockStatic(MediaTypeResolver.class);
        MockedStatic<AttachmentDataExtractor> extractor =
            mockStatic(AttachmentDataExtractor.class)) {

      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(true);

      // MediaTypeResolver returns empty map = no entity has the annotation
      resolver
          .when(
              () ->
                  MediaTypeResolver.getAcceptableMediaTypesFromEntity(
                      any(CdsModel.class), anyList()))
          .thenReturn(Map.of());

      // Verify that AttachmentDataExtractor is never called (early return)
      assertDoesNotThrow(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));

      extractor.verifyNoInteractions();
    }
  }

  @Test
  void doesNotThrow_whenNoFiles() {
    CdsEntity entity = mockEntity("Entity");

    Map<String, List<String>> allowed = Map.of(ATTACHMENTS_ENTITY, List.of("image/png"));

    try (MockedStatic<ApplicationHandlerHelper> helper =
            mockStatic(ApplicationHandlerHelper.class);
        MockedStatic<MediaTypeResolver> resolver = mockStatic(MediaTypeResolver.class);
        MockedStatic<AttachmentDataExtractor> extractor =
            mockStatic(AttachmentDataExtractor.class)) {
      CdsRuntime runtime = mockRuntime(entity);
      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(true);

      resolver
          .when(
              () ->
                  MediaTypeResolver.getAcceptableMediaTypesFromEntity(
                      any(CdsModel.class), anyList()))
          .thenReturn(allowed);

      extractor
          .when(
              () -> AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, List.of()))
          .thenReturn(null);

      assertDoesNotThrow(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));
    }
  }

  @ParameterizedTest
  @MethodSource("validFileScenarios")
  void doesNotThrow_whenFilesAreValid(boolean isMediaEntity) {

    CdsEntity entity = mockEntity("Entity");
    CdsRuntime runtime = mockRuntime(entity);

    Map<String, List<String>> allowed = Map.of(ATTACHMENTS_ENTITY, List.of("image/png"));
    Map<String, Set<String>> files = Map.of(ATTACHMENTS_ENTITY, Set.of("file.png"));

    try (MockedStatic<ApplicationHandlerHelper> helper =
            mockStatic(ApplicationHandlerHelper.class);
        MockedStatic<MediaTypeResolver> resolver = mockStatic(MediaTypeResolver.class);
        MockedStatic<AttachmentDataExtractor> extractor =
            mockStatic(AttachmentDataExtractor.class)) {

      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(isMediaEntity);
      setupMockCascader(entity, runtime.getCdsModel(), !isMediaEntity);

      resolver
          .when(
              () ->
                  MediaTypeResolver.getAcceptableMediaTypesFromEntity(
                      any(CdsModel.class), anyList()))
          .thenReturn(allowed);

      extractor
          .when(
              () -> AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, List.of()))
          .thenReturn(files);

      assertDoesNotThrow(
          () -> AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));
    }
  }

  private static Stream<Arguments> validFileScenarios() {
    return Stream.of(
        Arguments.of(true), // media entity
        Arguments.of(false) // attachment path
        );
  }

  @ParameterizedTest
  @MethodSource("invalidFileScenarios")
  void throwsException_whenFilesAreInvalid(boolean isMediaEntity) {

    CdsEntity entity = mockEntity("Entity");
    CdsRuntime runtime = mockRuntime(entity);

    Map<String, List<String>> allowed = Map.of(ATTACHMENTS_ENTITY, List.of("image/png"));
    Map<String, Set<String>> files = Map.of(ATTACHMENTS_ENTITY, Set.of("file.txt"));

    try (MockedStatic<ApplicationHandlerHelper> helper =
            mockStatic(ApplicationHandlerHelper.class);
        MockedStatic<MediaTypeResolver> resolver = mockStatic(MediaTypeResolver.class);
        MockedStatic<AttachmentDataExtractor> extractor =
            mockStatic(AttachmentDataExtractor.class)) {

      helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(isMediaEntity);
      setupMockCascader(entity, runtime.getCdsModel(), !isMediaEntity);

      resolver
          .when(
              () ->
                  MediaTypeResolver.getAcceptableMediaTypesFromEntity(
                      any(CdsModel.class), anyList()))
          .thenReturn(allowed);

      extractor
          .when(
              () -> AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, List.of()))
          .thenReturn(files);

      ServiceException ex =
          assertThrows(
              ServiceException.class,
              () ->
                  AttachmentValidationHelper.validateMediaAttachments(entity, List.of(), runtime));

      assertTrue(ex.getMessage().contains("Unsupported file types detected"));
    }
  }

  private static Stream<org.junit.jupiter.params.provider.Arguments> invalidFileScenarios() {
    return Stream.of(Arguments.of(true), Arguments.of(false));
  }

  private void setupMockCascader(CdsEntity entity, CdsModel model, boolean hasAttachments) {
    AssociationCascader cascader = mock(AssociationCascader.class);
    List<String> names = hasAttachments ? List.of(ATTACHMENTS_ENTITY) : List.of();
    when(cascader.findMediaEntityNames(model, entity)).thenReturn(names);
    AttachmentValidationHelper.setCascader(cascader);
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

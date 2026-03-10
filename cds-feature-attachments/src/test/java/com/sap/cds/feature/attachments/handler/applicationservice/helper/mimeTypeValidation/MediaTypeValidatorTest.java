/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.mimeTypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class MediaTypeValidatorTest {

  @AfterEach
  void reset() {
    MediaTypeValidator.setCascader(new AssociationCascader());
  }

  // ====================================================================================
  // Tests for validateMediaAttachments (previously AttachmentValidationHelperTest)
  // ====================================================================================

  @Nested
  class ValidateMediaAttachments {

    @Test
    void doesNothing_whenEntityIsNull() {
      assertDoesNotThrow(() -> MediaTypeValidator.validateMediaAttachments(null, List.of(), null));
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
            () -> MediaTypeValidator.validateMediaAttachments(entity, List.of(), runtime));
      }
    }

    @Test
    void doesNotThrow_whenNoFiles() {
      CdsEntity entity = mockEntity("Entity");

      try (MockedStatic<ApplicationHandlerHelper> helper =
              mockStatic(ApplicationHandlerHelper.class);
          MockedStatic<AttachmentDataExtractor> extractor =
              mockStatic(AttachmentDataExtractor.class)) {
        CdsRuntime runtime = mockRuntime(entity);
        helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(true);

        // Setup cascader so getAcceptableMediaTypesFromEntity works
        setupMockCascaderWithMediaEntities(
            entity, runtime.getCdsModel(), List.of("Entity.attachments"));
        CdsEntity mediaEntity = mockMediaEntityWithAnnotation("Entity.attachments", "image/png");
        when(runtime.getCdsModel().getEntity("Entity.attachments")).thenReturn(mediaEntity);

        extractor
            .when(
                () ->
                    AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, List.of()))
            .thenReturn(null);

        assertDoesNotThrow(
            () -> MediaTypeValidator.validateMediaAttachments(entity, List.of(), runtime));
      }
    }

    @ParameterizedTest
    @MethodSource("validFileScenarios")
    void doesNotThrow_whenFilesAreValid(boolean isMediaEntity, boolean hasAttachmentPath) {

      CdsEntity entity = mockEntity("Entity");
      CdsRuntime runtime = mockRuntime(entity);

      Map<String, Set<String>> files = Map.of("Entity.attachments", Set.of("file.png"));

      try (MockedStatic<ApplicationHandlerHelper> helper =
              mockStatic(ApplicationHandlerHelper.class);
          MockedStatic<AttachmentDataExtractor> extractor =
              mockStatic(AttachmentDataExtractor.class)) {

        helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(isMediaEntity);
        setupMockCascaderWithMediaEntities(
            entity,
            runtime.getCdsModel(),
            hasAttachmentPath ? List.of("Entity.attachments") : List.of());
        CdsEntity mediaEntity = mockMediaEntityWithAnnotation("Entity.attachments", "image/png");
        when(runtime.getCdsModel().getEntity("Entity.attachments")).thenReturn(mediaEntity);

        extractor
            .when(
                () ->
                    AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, List.of()))
            .thenReturn(files);

        assertDoesNotThrow(
            () -> MediaTypeValidator.validateMediaAttachments(entity, List.of(), runtime));
      }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validFileScenarios() {
      return Stream.of(
          org.junit.jupiter.params.provider.Arguments.of(true, false), // media entity
          org.junit.jupiter.params.provider.Arguments.of(false, true) // attachment path
          );
    }

    @ParameterizedTest
    @MethodSource("invalidFileScenarios")
    void throwsException_whenFilesAreInvalid(boolean isMediaEntity, boolean hasAttachmentPath) {

      CdsEntity entity = mockEntity("Entity");
      CdsRuntime runtime = mockRuntime(entity);

      Map<String, Set<String>> files = Map.of("Entity.attachments", Set.of("file.txt"));

      try (MockedStatic<ApplicationHandlerHelper> helper =
              mockStatic(ApplicationHandlerHelper.class);
          MockedStatic<AttachmentDataExtractor> extractor =
              mockStatic(AttachmentDataExtractor.class)) {

        helper.when(() -> ApplicationHandlerHelper.isMediaEntity(entity)).thenReturn(isMediaEntity);
        // Both cases need media entities to be found by the cascader for validation to trigger
        setupMockCascaderWithMediaEntities(
            entity, runtime.getCdsModel(), List.of("Entity.attachments"));
        CdsEntity mediaEntity = mockMediaEntityWithAnnotation("Entity.attachments", "image/png");
        when(runtime.getCdsModel().getEntity("Entity.attachments")).thenReturn(mediaEntity);

        extractor
            .when(
                () ->
                    AttachmentDataExtractor.extractAndValidateFileNamesByElement(entity, List.of()))
            .thenReturn(files);

        ServiceException ex =
            assertThrows(
                ServiceException.class,
                () -> MediaTypeValidator.validateMediaAttachments(entity, List.of(), runtime));

        assertThat(ex.getMessage()).contains("Unsupported media type");
        assertThat(ex.getMessage()).contains("Allowed types:");
      }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidFileScenarios() {
      return Stream.of(
          org.junit.jupiter.params.provider.Arguments.of(true, false),
          org.junit.jupiter.params.provider.Arguments.of(false, true));
    }
  }

  // ====================================================================================
  // Tests for resolveMimeType (previously MediaTypeServiceTest)
  // ====================================================================================

  @Nested
  class ResolveMimeType {

    @Test
    void returnsCorrectMimeType_forKnownExtension() {
      String result = MediaTypeValidator.resolveMimeType("file.png");
      assertThat(result).isEqualTo("image/png");
    }

    @Test
    void returnsCorrectMimeType_caseInsensitive() {
      String result = MediaTypeValidator.resolveMimeType("file.JPG");
      assertThat(result).isEqualTo("image/jpeg");
    }

    @Test
    void returnsDefaultMimeType_forUnknownExtension() {
      String result = MediaTypeValidator.resolveMimeType("file.unknown");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void returnsDefaultMimeType_whenNoExtensionPresent() {
      String result = MediaTypeValidator.resolveMimeType("file");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void returnsLastExtension_whenMultipleDotsPresent() {
      String result = MediaTypeValidator.resolveMimeType("archive.tar.gz");
      assertThat(result).isEqualTo("application/gzip");
    }

    @Test
    void handlesDoubleDotFiles() {
      String result = MediaTypeValidator.resolveMimeType("file..png");
      assertThat(result).isEqualTo("image/png");
    }

    @Test
    void handlesTrailingDotFile() {
      String result = MediaTypeValidator.resolveMimeType("file.");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void handlesHiddenDotFile() {
      String result = MediaTypeValidator.resolveMimeType(".gitignore");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void handlesOnlyDotsFile() {
      String result = MediaTypeValidator.resolveMimeType("...");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void handlesWeirdFilename() {
      String result = MediaTypeValidator.resolveMimeType("file..unknown");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void throws_whenFilenameIsNull() {
      assertThrows(ServiceException.class, () -> MediaTypeValidator.resolveMimeType(null));
    }

    @Test
    void throws_whenFileNameIsBlank() {
      assertThrows(ServiceException.class, () -> MediaTypeValidator.resolveMimeType("   "));
    }
  }

  // ====================================================================================
  // Tests for isMimeTypeAllowed (previously MediaTypeServiceTest)
  // ====================================================================================

  @Nested
  class IsMimeTypeAllowed {

    @Test
    void returnsFalse_whenMimeTypeIsNull() {
      assertFalse(MediaTypeValidator.isMimeTypeAllowed(List.of("image/png"), null));
    }

    @Test
    void returnsTrue_whenAcceptableTypesIsNull() {
      assertTrue(MediaTypeValidator.isMimeTypeAllowed(null, "image/png"));
    }

    @Test
    void returnsTrue_whenAcceptableTypesIsEmpty() {
      assertTrue(MediaTypeValidator.isMimeTypeAllowed(List.of(), "image/png"));
    }

    @Test
    void returnsTrue_whenWildcardAllPresent() {
      assertTrue(MediaTypeValidator.isMimeTypeAllowed(List.of("*/*"), "application/json"));
    }

    @Test
    void returnsTrue_forExactMatch() {
      assertTrue(MediaTypeValidator.isMimeTypeAllowed(List.of("image/png"), "image/png"));
    }

    @Test
    void returnsFalse_forDifferentMimeType() {
      assertFalse(MediaTypeValidator.isMimeTypeAllowed(List.of("image/png"), "image/jpeg"));
    }

    @Test
    void returnsTrue_forWildcardTypeMatch() {
      assertTrue(MediaTypeValidator.isMimeTypeAllowed(List.of("image/*"), "image/jpeg"));
    }

    @Test
    void returnsFalse_forNonMatchingWildcardType() {
      assertFalse(MediaTypeValidator.isMimeTypeAllowed(List.of("image/*"), "application/json"));
    }

    @Test
    void trimsAndNormalizesMimeTypes() {
      assertTrue(MediaTypeValidator.isMimeTypeAllowed(List.of(" IMAGE/PNG "), " image/png "));
    }

    @Test
    void returnsTrue_whenOneOfMultipleMatches() {
      assertTrue(
          MediaTypeValidator.isMimeTypeAllowed(
              List.of("application/json", "image/png"), "image/png"));
    }

    @Test
    void wildcardSubtype_rejectsApplicationPdf() {
      // image/* should NOT match application/pdf
      assertFalse(MediaTypeValidator.isMimeTypeAllowed(List.of("image/*"), "application/pdf"));
    }

    @Test
    void wildcardSubtype_rejectsDifferentCategory() {
      // text/* should NOT match image/png
      assertFalse(MediaTypeValidator.isMimeTypeAllowed(List.of("text/*"), "image/png"));
    }
  }

  // ====================================================================================
  // Tests for dotfiles (reviewer request)
  // ====================================================================================

  @Nested
  class DotFileValidation {

    @Test
    void dotfile_gitignore_resolvesToDefaultMimeType() {
      String result = MediaTypeValidator.resolveMimeType(".gitignore");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void dotfile_ssh_resolvesToDefaultMimeType() {
      String result = MediaTypeValidator.resolveMimeType(".ssh");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void dotfile_zshrc_resolvesToDefaultMimeType() {
      String result = MediaTypeValidator.resolveMimeType(".zshrc");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void dotfile_env_resolvesToDefaultMimeType() {
      String result = MediaTypeValidator.resolveMimeType(".env");
      assertThat(result).isEqualTo(MediaTypeValidator.DEFAULT_MEDIA_TYPE);
    }

    @Test
    void dotfile_isRejected_whenOnlyImageTypesAllowed() {
      // .gitignore resolves to application/octet-stream which is not image/*
      String mime = MediaTypeValidator.resolveMimeType(".gitignore");
      assertFalse(MediaTypeValidator.isMimeTypeAllowed(List.of("image/jpeg", "image/png"), mime));
    }
  }

  // ====================================================================================
  // Tests for getAcceptableMediaTypesFromEntity (previously MediaTypeResolverTest)
  // ====================================================================================

  @Nested
  class ResolveAcceptableMediaTypes {

    @Test
    void shouldReturnEmptyMapWhenNoMediaEntitiesFound() {
      AssociationCascader mockCascader = mock(AssociationCascader.class);
      MediaTypeValidator.setCascader(mockCascader);

      CdsModel model = mock(CdsModel.class);
      CdsEntity root = mock(CdsEntity.class);

      when(mockCascader.findMediaEntityNames(model, root)).thenReturn(List.of());

      Map<String, List<String>> result =
          MediaTypeValidator.getAcceptableMediaTypesFromEntity(root, model);

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
      MediaTypeValidator.setCascader(cascader);

      when(cascader.findMediaEntityNames(model, root)).thenReturn(List.of("MediaEntity"));
      when(model.getEntity("MediaEntity")).thenReturn(media);

      when(media.getElement("content")).thenReturn(element);
      when(element.findAnnotation("Core.AcceptableMediaTypes")).thenReturn(Optional.of(annotation));
      when(annotation.getValue()).thenReturn(List.of("image/png", "image/jpeg"));

      Map<String, List<String>> result =
          MediaTypeValidator.getAcceptableMediaTypesFromEntity(root, model);

      assertThat(result.get("MediaEntity")).containsExactly("image/png", "image/jpeg");
    }

    @Test
    void shouldResolveMediaTypesUsingCascader() {
      try (MockedStatic<ApplicationHandlerHelper> mocked =
          mockStatic(ApplicationHandlerHelper.class)) {

        CdsModel model = mock(CdsModel.class);
        CdsEntity root = mock(CdsEntity.class);
        CdsEntity media = mock(CdsEntity.class);
        AssociationCascader mockCascader = mock(AssociationCascader.class);
        MediaTypeValidator.setCascader(mockCascader);

        mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(any())).thenReturn(false);
        when(mockCascader.findMediaEntityNames(model, root)).thenReturn(List.of("MediaEntity"));
        when(model.getEntity("MediaEntity")).thenReturn(media);
        when(media.getElement(any())).thenReturn(null);

        Map<String, List<String>> result =
            MediaTypeValidator.getAcceptableMediaTypesFromEntity(root, model);

        assertThat(result).containsKey("MediaEntity");
      }
    }
  }

  // ====================================================================================
  // Shared test helpers
  // ====================================================================================

  private void setupMockCascader(CdsEntity entity, CdsModel model, boolean hasAttachmentPath) {
    AssociationCascader cascader = mock(AssociationCascader.class);
    when(cascader.hasAttachmentPath(model, entity)).thenReturn(hasAttachmentPath);
    MediaTypeValidator.setCascader(cascader);
  }

  private void setupMockCascaderWithMediaEntities(
      CdsEntity entity, CdsModel model, List<String> mediaEntityNames) {
    AssociationCascader cascader = mock(AssociationCascader.class);
    when(cascader.hasAttachmentPath(model, entity)).thenReturn(!mediaEntityNames.isEmpty());
    when(cascader.findMediaEntityNames(model, entity)).thenReturn(mediaEntityNames);
    MediaTypeValidator.setCascader(cascader);
  }

  @SuppressWarnings("unchecked")
  private CdsEntity mockMediaEntityWithAnnotation(String name, String... allowedTypes) {
    CdsEntity mediaEntity = mock(CdsEntity.class);
    when(mediaEntity.getQualifiedName()).thenReturn(name);

    CdsElement contentElement = mock(CdsElement.class);
    when(mediaEntity.getElement("content")).thenReturn(contentElement);

    CdsAnnotation<Object> annotation = mock(CdsAnnotation.class);
    when(annotation.getValue()).thenReturn(List.of(allowedTypes));
    when(contentElement.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));

    return mediaEntity;
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

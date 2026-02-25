/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.runtime.CdsRuntime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class AttachmentValidationHelperTest {
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
  void shouldDetectMimeTypeFromURLConnection() {
    assertMediaType("document.pdf", List.of("application/pdf"), "application/pdf");
  }

  @Test
  void shouldUseExtensionFallbackWhenURLConnectionFails() {
    assertMediaType("image.webp", List.of("image/webp"), "image/webp");
  }

  @Test
  void shouldSupportWildcardSubtype() {
    assertMediaType("image.jpeg", List.of("image/*"), "image/jpeg");
  }

  @Test
  void shouldAllowAllWithStarSlashStar() {
    assertMediaType("anyfile.jpeg", List.of("*/*"), "image/jpeg");
  }

  @Test
  void shouldAllowUnknownExtensionWithStarSlashStar() {
    assertMediaType(
        "anyfile.anyext", List.of("*/*"), AttachmentValidationHelper.DEFAULT_MEDIA_TYPE);
  }

  @Test
  void shouldAllowUnknownExtensionWhenAcceptableTypesNull() {
    assertMediaType("anyfile.anyext", null, AttachmentValidationHelper.DEFAULT_MEDIA_TYPE);
  }

  @Test
  void shouldAllowAllWhenAcceptableTypesEmpty() {
    assertMediaType("anyfile.anyext", List.of(), AttachmentValidationHelper.DEFAULT_MEDIA_TYPE);
  }

  @Test
  void shouldUseDefaultWhenUnknownExtensionExplicitlyAllowed() {
    assertMediaType(
        "file.unknownext",
        List.of(AttachmentValidationHelper.DEFAULT_MEDIA_TYPE),
        AttachmentValidationHelper.DEFAULT_MEDIA_TYPE);
  }

  @Test
  void shouldHandleUppercaseExtension() {
    assertMediaType("photo.JPG", List.of("image/jpeg"), "image/jpeg");
  }

  @Test
  void shouldRejectUnknownExtensionWhenNotAllowed() {
    assertUnsupported("anyfile.anyext", List.of("application/pdf"));
  }

  @Test
  void shouldThrowWhenFilenameHasNoExtension() {
    assertUnsupported("invalidfilename", List.of("application/pdf"));
  }

  @Test
  void shouldThrowWhenFilenameEndsWithDot() {
    assertUnsupported("file.", List.of("application/pdf"));
  }

  @Test
  void shouldThrowWhenMimeTypeNotAllowed() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AttachmentValidationHelper.validateMediaTypeForAttachment(
                    "document.pdf", List.of("image/png")));

    assertTrue(ex.getMessage().contains("not allowed"));
  }

  @Test
  void shouldThrowWhenDefaultMimeTypeNotAllowed() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AttachmentValidationHelper.validateMediaTypeForAttachment(
                    "file.unknownext", List.of("application/pdf")));

    assertTrue(ex.getMessage().contains("not allowed"));
  }

  @Test
  void shouldThrowWhenFileNameIsNull() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AttachmentValidationHelper.validateMediaTypeForAttachment(
                    null, List.of("application/pdf")));

    assertEquals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, ex.getErrorStatus());
    assertTrue(ex.getMessage().contains("must not be null or blank"));
  }

  @Test
  void shouldThrowWhenFileNameIsEmpty() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AttachmentValidationHelper.validateMediaTypeForAttachment(
                    "", List.of("application/pdf")));

    assertEquals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, ex.getErrorStatus());
  }

  @Test
  void shouldThrowWhenMimeTypeDoesNotMatchWildcard() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AttachmentValidationHelper.validateMediaTypeForAttachment(
                    "test.pdf", List.of("image/*")));

    assertEquals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, ex.getErrorStatus());
  }

  @Test
  void shouldThrowWhenFileNameIsBlank() {
    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () ->
                AttachmentValidationHelper.validateMediaTypeForAttachment(
                    "   ", List.of("application/pdf")));

    assertEquals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, ex.getErrorStatus());
  }

  @Test
  void shouldReturnFalseWhenMimeTypeIsNull() {
    boolean result = AttachmentValidationHelper.checkMimeTypeMatch(List.of("image/png"), null);

    assertFalse(result);
  }

  private void assertMediaType(String fileName, List<String> allowed, String expectedType) {

    String result = AttachmentValidationHelper.validateMediaTypeForAttachment(fileName, allowed);

    assertEquals(expectedType, result);
  }

  private void assertUnsupported(String fileName, List<String> allowed) {

    ServiceException ex =
        assertThrows(
            ServiceException.class,
            () -> AttachmentValidationHelper.validateMediaTypeForAttachment(fileName, allowed));

    assertEquals(ErrorStatuses.UNSUPPORTED_MEDIA_TYPE, ex.getErrorStatus());
  }

  @Test
  void shouldHandleDotFiles() {
    assertUnsupported(".gitignore", List.of("text/plain", "application/octet-stream"));
    assertUnsupported(".ssh", List.of("application/octet-stream"));
    assertUnsupported(".dockerignore", List.of("text/plain", "application/octet-stream"));
    assertUnsupported(".invalid.ext", List.of("application/pdf"));
  }

  @Test
  void validateAcceptableMediaTypes_shouldReturnWhenEntityIsNull() {
    // given
    CdsRuntime runtime = mock(CdsRuntime.class);

    // when / then
    assertDoesNotThrow(
        () -> AttachmentValidationHelper.validateAcceptableMediaTypes(null, List.of(), runtime));

    // ensure no further interaction happens
    verifyNoInteractions(runtime);
  }

  @Test
  void validateAcceptableMediaTypes_whenNotMediaEntity_returns() {
    when(entity.getQualifiedName()).thenReturn("Test.Entity");
    when(cdsModel.findEntity("Test.Entity")).thenReturn(Optional.of(serviceEntity));

    // IMPORTANT: force isMediaEntity = false
    try (MockedStatic<ApplicationHandlerHelper> mocked =
        mockStatic(ApplicationHandlerHelper.class)) {

      mocked.when(() -> ApplicationHandlerHelper.isMediaEntity(serviceEntity)).thenReturn(false);

      assertDoesNotThrow(
          () ->
              AttachmentValidationHelper.validateAcceptableMediaTypes(
                  entity, List.of(data), cdsRuntime));
    }
  }

  @Test
  void getEntityAcceptableMediaTypes_returnsAnnotationValue() {
    List<String> expectedTypes = List.of("image/png", "application/pdf");

    when(entity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(expectedTypes);
    assertEquals(expectedTypes, AttachmentValidationHelper.getEntityAcceptableMediaTypes(entity));
  }

  @Test
  void getEntityAcceptableMediaTypes_missingAnnotation_returnsWildcard() {
    when(entity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes")).thenReturn(Optional.empty());
    assertEquals(List.of("*/*"), AttachmentValidationHelper.getEntityAcceptableMediaTypes(entity));
  }

  @Test
  void getEntityAcceptableMediaTypes_nullAnnotationValue_returnsWildcard() {
    when(entity.getElement("content")).thenReturn(cdsElement);
    when(cdsElement.findAnnotation("Core.AcceptableMediaTypes"))
        .thenReturn(Optional.of(annotation));
    when(annotation.getValue()).thenReturn(null);
    assertEquals(List.of("*/*"), AttachmentValidationHelper.getEntityAcceptableMediaTypes(entity));
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
      String result = AttachmentValidationHelper.extractFileName(entity, List.of(data));
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
          () -> AttachmentValidationHelper.extractFileName(entity, List.of(data)));
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
          () -> AttachmentValidationHelper.extractFileName(entity, List.of(data)));
    }
  }

  @Test
  void validateAcceptableMediaTypes_nonMediaOrMissingEntity_doesNothing() {
    when(entity.getQualifiedName()).thenReturn("TestService.Roots");
    when(cdsModel.findEntity("TestService.Roots")).thenReturn(Optional.empty());

    assertDoesNotThrow(
        () ->
            AttachmentValidationHelper.validateAcceptableMediaTypes(entity, List.of(), cdsRuntime));
  }

  @Test
  void shouldNotThrowWhenEntityNotFoundInModel() {
    when(entity.getQualifiedName()).thenReturn("Test.Entity");
    when(cdsModel.findEntity("Test.Entity")).thenReturn(Optional.empty());

    assertDoesNotThrow(
        () ->
            AttachmentValidationHelper.validateAcceptableMediaTypes(entity, List.of(), cdsRuntime));
  }

  @Test
  void validateAcceptableMediaTypes_mediaTypeMatches_succeeds() {
    setupMediaEntity(List.of("image/png"));
    try (MockedStatic<AttachmentValidationHelper> helperStatic =
        mockStatic(AttachmentValidationHelper.class, CALLS_REAL_METHODS)) {

      // file.png → media type image/png → allowed
      helperStatic
          .when(() -> AttachmentValidationHelper.extractFileName(entity, List.of(data)))
          .thenReturn("file.png");

      assertDoesNotThrow(
          () ->
              AttachmentValidationHelper.validateAcceptableMediaTypes(
                  entity, List.of(data), cdsRuntime));
    }
  }

  @Test
  void validateAcceptableMediaTypes_mediaTypeMismatch_throws() {
    setupMediaEntity(List.of("image/png"));
    try (MockedStatic<AttachmentValidationHelper> helperStatic =
        mockStatic(AttachmentValidationHelper.class, CALLS_REAL_METHODS)) {

      // file.jpg → media type image/jpeg → NOT allowed
      helperStatic
          .when(() -> AttachmentValidationHelper.extractFileName(entity, List.of(data)))
          .thenReturn("file.jpg");

      assertThrows(
          ServiceException.class,
          () ->
              AttachmentValidationHelper.validateAcceptableMediaTypes(
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

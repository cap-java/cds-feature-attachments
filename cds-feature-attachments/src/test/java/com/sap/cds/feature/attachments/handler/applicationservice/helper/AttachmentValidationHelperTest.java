/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.junit.jupiter.api.Assertions.*;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttachmentValidationHelperTest {

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
}

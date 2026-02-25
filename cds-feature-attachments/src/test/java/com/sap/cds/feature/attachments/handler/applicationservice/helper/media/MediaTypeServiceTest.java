/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.media;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class MediaTypeServiceTest {

  @Test
  void returnsCorrectMimeType_forKnownExtension() {
    String result = MediaTypeService.resolveMimeType("file.png");

    assertEquals("image/png", result);
  }

  @Test
  void returnsCorrectMimeType_caseInsensitive() {
    String result = MediaTypeService.resolveMimeType("file.JPG");

    assertEquals("image/jpeg", result);
  }

  @Test
  void returnsDefaultMimeType_forUnknownExtension() {
    String result = MediaTypeService.resolveMimeType("file.unknown");

    assertEquals(MediaTypeService.DEFAULT_MEDIA_TYPE, result);
  }

  @Test
  void returnsDefaultMimeType_whenNoExtensionPresent() {
    String result = MediaTypeService.resolveMimeType("file");

    assertEquals(MediaTypeService.DEFAULT_MEDIA_TYPE, result);
  }

  @Test
  void returnsLastExtension_whenMultipleDotsPresent() {
    String result = MediaTypeService.resolveMimeType("archive.tar.gz");

    assertEquals("application/gzip", result);
  }

  @Test
  void handlesDoubleDotFiles() {
    String result = MediaTypeService.resolveMimeType("file..png");

    assertEquals("image/png", result);
  }

  @Test
  void handlesTrailingDotFile() {
    String result = MediaTypeService.resolveMimeType("file.");

    assertEquals(MediaTypeService.DEFAULT_MEDIA_TYPE, result);
  }

  @Test
  void handlesHiddenDotFile() {
    String result = MediaTypeService.resolveMimeType(".gitignore");

    assertEquals(MediaTypeService.DEFAULT_MEDIA_TYPE, result);
  }

  @Test
  void handlesOnlyDotsFile() {
    String result = MediaTypeService.resolveMimeType("...");

    assertEquals(MediaTypeService.DEFAULT_MEDIA_TYPE, result);
  }

  @Test
  void handlesWeirdFilename() {
    String result = MediaTypeService.resolveMimeType("file..unknown");

    assertEquals(MediaTypeService.DEFAULT_MEDIA_TYPE, result);
  }

  @Test
  void returnsFalse_whenMimeTypeIsNull() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of("image/png"), null);

    assertFalse(result);
  }

  @Test
  void returnsTrue_whenAcceptableTypesIsNull() {
    boolean result = MediaTypeService.isMimeTypeAllowed(null, "image/png");

    assertTrue(result);
  }

  @Test
  void returnsTrue_whenAcceptableTypesIsEmpty() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of(), "image/png");

    assertTrue(result);
  }

  @Test
  void returnsTrue_whenWildcardAllPresent() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of("*/*"), "application/json");

    assertTrue(result);
  }

  @Test
  void returnsTrue_forExactMatch() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of("image/png"), "image/png");

    assertTrue(result);
  }

  @Test
  void returnsFalse_forDifferentMimeType() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of("image/png"), "image/jpeg");

    assertFalse(result);
  }

  @Test
  void returnsTrue_forWildcardTypeMatch() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of("image/*"), "image/jpeg");

    assertTrue(result);
  }

  @Test
  void returnsFalse_forNonMatchingWildcardType() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of("image/*"), "application/json");

    assertFalse(result);
  }

  @Test
  void trimsAndNormalizesMimeTypes() {
    boolean result = MediaTypeService.isMimeTypeAllowed(List.of(" IMAGE/PNG "), " image/png ");

    assertTrue(result);
  }

  @Test
  void returnsTrue_whenOneOfMultipleMatches() {
    boolean result =
        MediaTypeService.isMimeTypeAllowed(List.of("application/json", "image/png"), "image/png");

    assertTrue(result);
  }
}

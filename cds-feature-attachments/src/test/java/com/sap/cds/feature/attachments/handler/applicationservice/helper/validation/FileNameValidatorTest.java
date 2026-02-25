/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper.validation;

import static org.junit.jupiter.api.Assertions.*;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import org.junit.jupiter.api.Test;

class FileNameValidatorTest {

  @Test
  void throwsException_whenFileNameIsNull() {
    ServiceException ex = assertThrows(ServiceException.class, () -> FileNameValidator.validateAndNormalize(null));

    assertEquals(ErrorStatuses.BAD_REQUEST, ex.getErrorStatus());
    assertTrue(ex.getMessage().contains("must not be null"));
  }

  @Test
  void throwsException_whenFileNameIsBlank() {
    ServiceException ex = assertThrows(ServiceException.class, () -> FileNameValidator.validateAndNormalize("   "));

    assertEquals(ErrorStatuses.BAD_REQUEST, ex.getErrorStatus());
    assertTrue(ex.getMessage().contains("must not be blank"));
  }

  @Test
  void throwsException_whenNoExtensionPresent() {
    ServiceException ex = assertThrows(ServiceException.class, () -> FileNameValidator.validateAndNormalize("file"));
    assertTrue(ex.getMessage().contains("Invalid filename format"));
  }

  @Test
  void throwsException_whenOnlyDotFile() {
    ServiceException ex = assertThrows(ServiceException.class, () -> FileNameValidator.validateAndNormalize("."));
    assertTrue(ex.getMessage().contains("Invalid filename format"));
  }

  @Test
  void throwsException_whenTrailingDot() {
    ServiceException ex = assertThrows(ServiceException.class, () -> FileNameValidator.validateAndNormalize("file."));
    assertTrue(ex.getMessage().contains("Invalid filename format"));
  }

  @Test
  void throwsException_whenOnlyDots() {
    ServiceException ex = assertThrows(ServiceException.class, () -> FileNameValidator.validateAndNormalize("..."));
    assertTrue(ex.getMessage().contains("Invalid filename format"));
  }

  @Test
  void throwsException_whenTrimmedBecomesInvalid() {
    ServiceException ex = assertThrows(
        ServiceException.class, () -> FileNameValidator.validateAndNormalize("   file   "));
    assertTrue(ex.getMessage().contains("Invalid filename format"));
  }

  @Test
  void doesNotThrow_whenValidFileName() {
    assertDoesNotThrow(() -> FileNameValidator.validateAndNormalize("file.txt"));
  }

  @Test
  void doesNotThrow_whenValidFileNameWithMultipleDots() {
    assertDoesNotThrow(() -> FileNameValidator.validateAndNormalize("archive.tar.gz"));
  }

  @Test
  void doesNotThrow_whenValidFileNameWithWhitespace() {
    assertDoesNotThrow(() -> FileNameValidator.validateAndNormalize("  file.txt  "));
  }

  @Test
  void doesNotThrow_whenDoubleDotButValidExtension() {
    assertDoesNotThrow(() -> FileNameValidator.validateAndNormalize("file..txt"));
  }

  @Test
  void doesNotThrow_whenHiddenFileWithoutName() {
    assertDoesNotThrow(() -> FileNameValidator.validateAndNormalize(".config.json"));
  }

}

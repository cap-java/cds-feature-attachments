/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cds.services.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestPersistenceHandlerTest {

  private TestPersistenceHandler testPersistenceHandler;

  @BeforeEach
  void setUp() {
    testPersistenceHandler = new TestPersistenceHandler();
  }

  @Test
  void testReset() {
    // Set both flags to true
    testPersistenceHandler.setThrowExceptionOnUpdate(true);
    testPersistenceHandler.setThrowExceptionOnCreate(true);

    // Reset should set both flags to false
    testPersistenceHandler.reset();

    // Verify no exceptions are thrown after reset
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnUpdate());
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnCreate());
  }

  @Test
  void testThrowExceptionOnUpdateWhenEnabled() {
    testPersistenceHandler.setThrowExceptionOnUpdate(true);

    ServiceException exception =
        assertThrows(ServiceException.class, () -> testPersistenceHandler.throwExceptionOnUpdate());

    assertTrue(exception.getMessage().contains("Exception on update"));
  }

  @Test
  void testThrowExceptionOnUpdateWhenDisabled() {
    testPersistenceHandler.setThrowExceptionOnUpdate(false);

    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnUpdate());
  }

  @Test
  void testThrowExceptionOnCreateWhenEnabled() {
    testPersistenceHandler.setThrowExceptionOnCreate(true);

    ServiceException exception =
        assertThrows(ServiceException.class, () -> testPersistenceHandler.throwExceptionOnCreate());

    assertTrue(exception.getMessage().contains("Exception on create"));
  }

  @Test
  void testThrowExceptionOnCreateWhenDisabled() {
    testPersistenceHandler.setThrowExceptionOnCreate(false);

    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnCreate());
  }

  @Test
  void testSetThrowExceptionOnUpdate() {
    // Test setting to true
    testPersistenceHandler.setThrowExceptionOnUpdate(true);
    assertThrows(ServiceException.class, () -> testPersistenceHandler.throwExceptionOnUpdate());

    // Test setting to false
    testPersistenceHandler.setThrowExceptionOnUpdate(false);
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnUpdate());
  }

  @Test
  void testSetThrowExceptionOnCreate() {
    // Test setting to true
    testPersistenceHandler.setThrowExceptionOnCreate(true);
    assertThrows(ServiceException.class, () -> testPersistenceHandler.throwExceptionOnCreate());

    // Test setting to false
    testPersistenceHandler.setThrowExceptionOnCreate(false);
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnCreate());
  }

  @Test
  void testDefaultBehavior() {
    // By default, both flags should be false
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnUpdate());
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnCreate());
  }

  @Test
  void testIndependentFlagBehavior() {
    // Test that the flags work independently
    testPersistenceHandler.setThrowExceptionOnUpdate(true);
    testPersistenceHandler.setThrowExceptionOnCreate(false);

    assertThrows(ServiceException.class, () -> testPersistenceHandler.throwExceptionOnUpdate());
    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnCreate());

    // Switch them
    testPersistenceHandler.setThrowExceptionOnUpdate(false);
    testPersistenceHandler.setThrowExceptionOnCreate(true);

    assertDoesNotThrow(() -> testPersistenceHandler.throwExceptionOnUpdate());
    assertThrows(ServiceException.class, () -> testPersistenceHandler.throwExceptionOnCreate());
  }
}

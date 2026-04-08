/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class OSClientTest {

  @Test
  void testDefaultDeleteContentByPrefixThrowsUnsupportedOperationException() {
    OSClient client =
        new OSClient() {
          @Override
          public java.util.concurrent.Future<Void> uploadContent(
              java.io.InputStream content, String completeFileName, String contentType) {
            return null;
          }

          @Override
          public java.util.concurrent.Future<Void> deleteContent(String completeFileName) {
            return null;
          }

          @Override
          public java.util.concurrent.Future<java.io.InputStream> readContent(
              String completeFileName) {
            return null;
          }
        };

    ExecutionException thrown =
        assertThrows(ExecutionException.class, () -> client.deleteContentByPrefix("prefix/").get());
    assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
  }
}

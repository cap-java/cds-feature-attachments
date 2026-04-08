/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/** The {@link OSClient} is the connection to the object store service. */
public interface OSClient {

  /**
   * Uploads content to the object store.
   *
   * @param content the input stream of the file content
   * @param completeFileName the object key under which to store the content
   * @param contentType the MIME type of the content
   * @return a {@link Future} that completes when the upload finishes
   */
  Future<Void> uploadContent(InputStream content, String completeFileName, String contentType);

  /**
   * Deletes a single object from the object store.
   *
   * @param completeFileName the object key to delete
   * @return a {@link Future} that completes when the deletion finishes
   */
  Future<Void> deleteContent(String completeFileName);

  /**
   * Reads the content of an object from the object store.
   *
   * @param completeFileName the object key to read
   * @return a {@link Future} containing the content as an {@link InputStream}
   */
  Future<InputStream> readContent(String completeFileName);

  /**
   * Deletes all objects whose keys start with the given prefix. Used for tenant cleanup in shared
   * multitenancy mode.
   *
   * @param prefix the key prefix to match (e.g. {@code "tenantId/"})
   * @return a {@link Future} that completes when all matching objects have been deleted
   */
  default Future<Void> deleteContentByPrefix(String prefix) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("deleteContentByPrefix not supported by this client"));
  }
}

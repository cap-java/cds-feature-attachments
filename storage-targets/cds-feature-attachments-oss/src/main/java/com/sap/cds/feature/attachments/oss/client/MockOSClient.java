/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class MockOSClient implements OSClient {

    @Override
    public CompletableFuture<Void> uploadContent(InputStream content, String completeFileName, String contentType) {
        // Mock: immediately complete
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> deleteContent(String completeFileName) {
        // Mock: immediately complete
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<InputStream> readContent(String completeFileName) {
        // Mock: immediately complete with null
        return CompletableFuture.completedFuture(null);
    }
}

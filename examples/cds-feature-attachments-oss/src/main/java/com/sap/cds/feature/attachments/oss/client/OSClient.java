package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * The {@link OSClient} is the connection to the object store service.
 */

 //todo add return values for the methods here
public interface OSClient {
    
    CompletableFuture<Void> uploadContent(InputStream content, String completeFileName, String contentType);

    CompletableFuture<Void> deleteContent(String completeFileName);

    CompletableFuture<InputStream> readContent(String completeFileName);
}

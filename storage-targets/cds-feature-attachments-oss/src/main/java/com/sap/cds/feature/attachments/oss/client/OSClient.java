package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * The {@link OSClient} is the connection to the object store service.
 */

public interface OSClient<T> {
    
    Future<T> uploadContent(InputStream content, String completeFileName, String contentType);

    Future<T> deleteContent(String completeFileName);

    Future<InputStream> readContent(String completeFileName);
}

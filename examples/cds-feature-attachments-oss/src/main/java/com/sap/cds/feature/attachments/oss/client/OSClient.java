package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;

/**
 * The {@link OSClient} is the connection to the object store service.
 */

 //todo add return values for the methods here
public interface OSClient {
    
    void uploadContent(InputStream content, String completeFileName);

    void deleteContent(String completeFileName);

    InputStream readContent(String completeFileName);
}

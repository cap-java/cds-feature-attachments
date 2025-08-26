/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * The {@link OSClient} is the connection to the object store service.
 */

public interface OSClient {
    
    Future<Void> uploadContent(InputStream content, String completeFileName, String contentType);

    Future<Void> deleteContent(String completeFileName);

    Future<InputStream> readContent(String completeFileName);
}

package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;

import com.sap.cds.services.environment.CdsProperties.ConnectionPool;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class AWSClient implements OSClient {
    public final AWSClientProvider clientProvider;

    public AWSClient(ServiceBinding binding, ConnectionPool configuration) {
        this.clientProvider = new AWSClientProvider(binding, configuration);
    }

    public void uploadContent(InputStream content){
        System.out.println("Upload content");
    }

    public void deleteContent(String identifier) {
        System.out.println("Delete content");
    }

    public void readContent(String identifier) {
        System.out.println("Read content");
    }
}

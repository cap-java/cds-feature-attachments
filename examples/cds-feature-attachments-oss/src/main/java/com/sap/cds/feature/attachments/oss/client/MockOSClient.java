package com.sap.cds.feature.attachments.oss.client;

import java.io.InputStream;

public class MockOSClient  implements OSClient {

    @Override
    public void uploadContent(InputStream content, String fileName) {
        System.out.println("MockOSClient: Uploading content (mock implementation)");
    }

    @Override
    public void deleteContent(String completeFileName) {
        System.out.println("MockOSClient: Deleting content with identifier: " + completeFileName);
    }

    @Override
    public InputStream readContent(String completeFileName) {
        System.out.println("MockOSClient: Reading content with identifier: " + completeFileName);
        return null; // Mock implementation returns null
    }
    
}

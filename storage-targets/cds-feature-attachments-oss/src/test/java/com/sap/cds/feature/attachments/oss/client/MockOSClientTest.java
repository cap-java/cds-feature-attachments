package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;

class MockOSClientTest {

    @Test
    void testConstructorWithNoBindingUsesMockClient() throws NoSuchFieldException, IllegalAccessException {
        ExecutorService executor = Executors.newCachedThreadPool();
        OSSAttachmentsServiceHandler handler = new OSSAttachmentsServiceHandler(Optional.empty(), executor);
        // Reflection to access private static osClient
        OSClient client = OSSAttachmentsServiceHandlerTestUtils.getOsClient(handler);
        assertInstanceOf(MockOSClient.class, client);
    }

    @Test
    void testUploadReadDeleteCalls() throws Exception {
        MockOSClient client = new MockOSClient();

        // Test uploadContent completes successfully
        InputStream input = new ByteArrayInputStream("test".getBytes());
        assertDoesNotThrow(() -> client.uploadContent(input, "file.txt", "text/plain").get());

        // Test readContent completes successfully and returns null (as per mock)
        InputStream result = client.readContent("file.txt").get();
        assertNull(result);

        // Test deleteContent completes successfully
        assertDoesNotThrow(() -> client.deleteContent("file.txt").get());
    }
}

/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MockOSClientTest {

  @Test
  void testConstructorWithNoBindingUsesMockClient()
      throws NoSuchFieldException, IllegalAccessException {
    ExecutorService executor = Executors.newCachedThreadPool();
    OSSAttachmentsServiceHandler handler =
        new OSSAttachmentsServiceHandler(Mockito.mock(ServiceBinding.class), executor);
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

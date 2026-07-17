/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.client;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandlerTestUtils;
import com.sap.cloud.environment.servicebinding.api.DefaultServiceBindingAccessor;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class AWSClientIT {

  @Test
  void testCreateReadDeleteAttachmentFlowAWS() {
    ServiceBinding binding = getObjectStoreBinding();
    assumeTrue(binding != null, "No AWS objectstore binding available");
    ExecutorService executor = Executors.newCachedThreadPool();
    OSSAttachmentsServiceHandlerTestUtils.testCreateReadDeleteAttachmentFlow(binding, executor);
  }

  private ServiceBinding getObjectStoreBinding() {
    return DefaultServiceBindingAccessor.getInstance().getServiceBindings().stream()
        .filter(b -> b.getServiceName().map("objectstore"::equals).orElse(false))
        .filter(b -> b.getCredentials().containsKey("access_key_id"))
        .findFirst()
        .orElse(null);
  }
}

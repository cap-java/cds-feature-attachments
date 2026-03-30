/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RegistrationTest {

  @Test
  void testEventHandlersRegistersOSSHandler() {
    // Arrange
    Registration registration = new Registration();
    CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    CdsEnvironment environment = mock(CdsEnvironment.class);
    ServiceBinding binding = mock(ServiceBinding.class);

    // Setup valid AWS credentials for the binding
    Map<String, Object> credentials = new HashMap<>();
    credentials.put("host", "aws.example.com");
    credentials.put("region", "us-east-1");
    credentials.put("access_key_id", "test-access-key");
    credentials.put("secret_access_key", "test-secret-key");
    credentials.put("bucket", "test-bucket");

    when(configurer.getCdsRuntime()).thenReturn(cdsRuntime);
    when(cdsRuntime.getEnvironment()).thenReturn(environment);
    when(binding.getServiceName()).thenReturn(Optional.of("objectstore"));
    when(binding.getCredentials()).thenReturn(credentials);
    when(environment.getServiceBindings()).thenReturn(Stream.of(binding));

    // Act
    registration.eventHandlers(configurer);

    // Assert: OSSAttachmentsServiceHandler should be registered
    verify(configurer).eventHandler(any(OSSAttachmentsServiceHandler.class));
  }
}

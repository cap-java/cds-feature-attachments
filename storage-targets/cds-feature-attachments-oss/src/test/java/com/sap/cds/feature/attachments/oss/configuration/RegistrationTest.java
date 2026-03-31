/*
 * © 2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Test
  void testEventHandlersRegistersCleanupHandlerWhenMultitenancyShared() {
    Registration registration = new Registration();
    CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    CdsEnvironment environment = mock(CdsEnvironment.class);
    ServiceBinding binding = mock(ServiceBinding.class);

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
    when(environment.getProperty("cds.multitenancy.enabled", Boolean.class, Boolean.FALSE))
        .thenReturn(Boolean.TRUE);
    when(environment.getProperty("cds.attachments.objectStore.kind", String.class, null))
        .thenReturn("shared");

    registration.eventHandlers(configurer);

    verify(configurer, times(2)).eventHandler(any());
  }

  @Test
  void testEventHandlersNoBindingDoesNotRegister() {
    Registration registration = new Registration();
    CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    CdsEnvironment environment = mock(CdsEnvironment.class);

    when(configurer.getCdsRuntime()).thenReturn(cdsRuntime);
    when(cdsRuntime.getEnvironment()).thenReturn(environment);
    when(environment.getServiceBindings()).thenReturn(Stream.empty());

    registration.eventHandlers(configurer);

    verify(configurer, never()).eventHandler(any());
  }

  @Test
  void testMtEnabledNonSharedKindRegistersOnlyOSSHandler() {
    Registration registration = new Registration();
    CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    CdsEnvironment environment = mock(CdsEnvironment.class);
    ServiceBinding binding = mock(ServiceBinding.class);

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
    when(environment.getProperty("cds.multitenancy.enabled", Boolean.class, Boolean.FALSE))
        .thenReturn(Boolean.TRUE);
    when(environment.getProperty("cds.attachments.objectStore.kind", String.class, null))
        .thenReturn("dedicated");

    registration.eventHandlers(configurer);

    verify(configurer, times(1)).eventHandler(any(OSSAttachmentsServiceHandler.class));
    verify(configurer, times(1)).eventHandler(any());
  }

  @Test
  void testMtEnabledNullKindRegistersOnlyOSSHandler() {
    Registration registration = new Registration();
    CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    CdsEnvironment environment = mock(CdsEnvironment.class);
    ServiceBinding binding = mock(ServiceBinding.class);

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
    when(environment.getProperty("cds.multitenancy.enabled", Boolean.class, Boolean.FALSE))
        .thenReturn(Boolean.TRUE);

    registration.eventHandlers(configurer);

    verify(configurer, times(1)).eventHandler(any(OSSAttachmentsServiceHandler.class));
    verify(configurer, times(1)).eventHandler(any());
  }

  @Test
  void testMtDisabledSharedKindRegistersOnlyOSSHandler() {
    Registration registration = new Registration();
    CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    CdsEnvironment environment = mock(CdsEnvironment.class);
    ServiceBinding binding = mock(ServiceBinding.class);

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
    when(environment.getProperty("cds.attachments.objectStore.kind", String.class, null))
        .thenReturn("shared");

    registration.eventHandlers(configurer);

    verify(configurer, times(1)).eventHandler(any(OSSAttachmentsServiceHandler.class));
    verify(configurer, times(1)).eventHandler(any());
  }
}

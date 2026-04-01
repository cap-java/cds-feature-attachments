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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistrationTest {

  private Registration registration;
  private CdsRuntimeConfigurer configurer;
  private CdsEnvironment environment;
  private ServiceBinding awsBinding;

  private static ServiceBinding createAwsBinding() {
    ServiceBinding binding = mock(ServiceBinding.class);
    Map<String, Object> credentials = new HashMap<>();
    credentials.put("host", "aws.example.com");
    credentials.put("region", "us-east-1");
    credentials.put("access_key_id", "test-access-key");
    credentials.put("secret_access_key", "test-secret-key");
    credentials.put("bucket", "test-bucket");
    when(binding.getServiceName()).thenReturn(Optional.of("objectstore"));
    when(binding.getCredentials()).thenReturn(credentials);
    return binding;
  }

  @BeforeEach
  void setup() {
    registration = new Registration();
    configurer = mock(CdsRuntimeConfigurer.class);
    CdsRuntime cdsRuntime = mock(CdsRuntime.class);
    environment = mock(CdsEnvironment.class);
    when(configurer.getCdsRuntime()).thenReturn(cdsRuntime);
    when(cdsRuntime.getEnvironment()).thenReturn(environment);
    awsBinding = createAwsBinding();
  }

  @Test
  void testEventHandlersRegistersOSSHandler() {
    when(environment.getServiceBindings()).thenReturn(Stream.of(awsBinding));

    registration.eventHandlers(configurer);

    verify(configurer).eventHandler(any(OSSAttachmentsServiceHandler.class));
  }

  @Test
  void testEventHandlersRegistersCleanupHandlerWhenMultitenancyShared() {
    when(environment.getServiceBindings()).thenReturn(Stream.of(awsBinding));
    when(environment.getProperty("cds.multitenancy.enabled", Boolean.class, Boolean.FALSE))
        .thenReturn(Boolean.TRUE);
    when(environment.getProperty("cds.attachments.objectStore.kind", String.class, null))
        .thenReturn("shared");

    registration.eventHandlers(configurer);

    verify(configurer, times(2)).eventHandler(any());
  }

  @Test
  void testEventHandlersNoBindingDoesNotRegister() {
    when(environment.getServiceBindings()).thenReturn(Stream.empty());

    registration.eventHandlers(configurer);

    verify(configurer, never()).eventHandler(any());
  }

  @Test
  void testMtEnabledNonSharedKindRegistersOnlyOSSHandler() {
    when(environment.getServiceBindings()).thenReturn(Stream.of(awsBinding));
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
    when(environment.getServiceBindings()).thenReturn(Stream.of(awsBinding));
    when(environment.getProperty("cds.multitenancy.enabled", Boolean.class, Boolean.FALSE))
        .thenReturn(Boolean.TRUE);

    registration.eventHandlers(configurer);

    verify(configurer, times(1)).eventHandler(any(OSSAttachmentsServiceHandler.class));
    verify(configurer, times(1)).eventHandler(any());
  }

  @Test
  void testMtDisabledSharedKindRegistersOnlyOSSHandler() {
    when(environment.getServiceBindings()).thenReturn(Stream.of(awsBinding));
    when(environment.getProperty("cds.attachments.objectStore.kind", String.class, null))
        .thenReturn("shared");

    registration.eventHandlers(configurer);

    verify(configurer, times(1)).eventHandler(any(OSSAttachmentsServiceHandler.class));
    verify(configurer, times(1)).eventHandler(any());
  }
}

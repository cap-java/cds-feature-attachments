package com.sap.cds.feature.attachments.oss.configuration;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.oss.handler.OSSAttachmentsServiceHandler;
import com.sap.cds.services.environment.CdsEnvironment;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

class RegistrationTest {

	@Test
    void testEventHandlersRegistersOSSHandler() {
        // Arrange
        Registration registration = new Registration();
        CdsRuntimeConfigurer configurer = mock(CdsRuntimeConfigurer.class);
        CdsRuntime cdsRuntime = mock(CdsRuntime.class);
        CdsEnvironment environment = mock(CdsEnvironment.class);
        ServiceBinding binding = mock(ServiceBinding.class);

        when(configurer.getCdsRuntime()).thenReturn(cdsRuntime);
        when(cdsRuntime.getEnvironment()).thenReturn(environment);
        when(binding.getServiceName()).thenReturn(Optional.of("objectstore"));
        when(environment.getServiceBindings()).thenReturn(Stream.of(binding));

        // Act
        registration.eventHandlers(configurer);

        // Assert: OSSAttachmentsServiceHandler should be registered
        verify(configurer).eventHandler(any(OSSAttachmentsServiceHandler.class));
    }

}

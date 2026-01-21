/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModifyApplicationHandlerHelperTest {

    private static CdsRuntime runtime;
    private ModifyAttachmentEventFactory eventFactory;
    private EventContext eventContext;
    private ParameterInfo parameterInfo;
    private Path path;
    private ResolvedSegment target;
    private ModifyAttachmentEvent event;

    @BeforeAll
    static void classSetup() {
        runtime = RuntimeHelper.runtime;
    }

    @BeforeEach
    void setup() {
        eventFactory = mock(ModifyAttachmentEventFactory.class);
        eventContext = mock(EventContext.class);
        parameterInfo = mock(ParameterInfo.class);
        path = mock(Path.class);
        target = mock(ResolvedSegment.class);
        event = mock(ModifyAttachmentEvent.class);
        when(eventContext.getParameterInfo()).thenReturn(parameterInfo);
        when(path.target()).thenReturn(target);
        when(eventFactory.getEvent(any(), any(), any())).thenReturn(event);
    }

    @Test
    void serviceExceptionDueToContentLength() {
        // Arrange: Get EventItems entity which has @Validation.Maximum: '10KB' on
        // sizeLimitedAttachments.content
        String attachmentEntityName = "unit.test.TestService.EventItems.sizeLimitedAttachments";
        CdsEntity entity = runtime.getCdsModel().findEntity(attachmentEntityName).orElseThrow();

        // Create attachment data
        var attachment = Attachments.create();
        attachment.setId(UUID.randomUUID().toString());
        attachment.setContent(mock(InputStream.class));

        // Setup path mock to return EventItems entity and attachment values
        when(target.entity()).thenReturn(entity);
        when(target.values()).thenReturn(attachment);
        when(target.keys()).thenReturn(Map.of(Attachments.ID, attachment.getId()));

        // Set Content-Length header to exceed 10KB (10240 bytes)
        when(parameterInfo.getHeader("Content-Length")).thenReturn("20000");

        var existingAttachments = List.of(attachment);

        // Act & Assert
        var exception = assertThrows(
                ServiceException.class,
                () -> ModifyApplicationHandlerHelper.handleAttachmentForEntity(
                        existingAttachments,
                        eventFactory,
                        eventContext,
                        path,
                        attachment.getContent(),
                        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));

        assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    }

    @Test
    void serviceExceptionDueToLimitExceeded() {
        // Arrange: Use the attachment entity with @Validation.Maximum: '10KB'
        String attachmentEntityName = "unit.test.TestService.EventItems.sizeLimitedAttachments";
        CdsEntity entity = runtime.getCdsModel().findEntity(attachmentEntityName).orElseThrow();

        var attachment = Attachments.create();
        attachment.setId(UUID.randomUUID().toString());

        // Content that exceeds 10KB (10240 bytes) when read
        byte[] largeContent = new byte[15000]; // 15KB
        var content = new ByteArrayInputStream(largeContent);
        attachment.setContent(content);

        when(target.entity()).thenReturn(entity);
        when(target.values()).thenReturn(attachment);
        when(target.keys()).thenReturn(Map.of(Attachments.ID, attachment.getId()));

        // NO Content-Length header - limit will be checked during streaming
        when(parameterInfo.getHeader("Content-Length")).thenReturn(null);

        // Make event.processEvent() read from the stream, triggering the limit check
        when(event.processEvent(any(), any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            InputStream wrappedContent = invocation.getArgument(1);
                            if (wrappedContent != null) {
                                // Read all bytes - this will trigger CountingInputStream to throw
                                byte[] buffer = new byte[1024];
                                while (wrappedContent.read(buffer) != -1) {
                                    // Keep reading until exception or EOF
                                }
                            }
                            return null;
                        });

        var existingAttachments = List.of(attachment);

        // Act & Assert
        var exception = assertThrows(
                ServiceException.class,
                () -> ModifyApplicationHandlerHelper.handleAttachmentForEntity(
                        existingAttachments,
                        eventFactory,
                        eventContext,
                        path,
                        content,
                        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));

        assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
    }

    @Test
    void defaultValMaxValueUsed() {
        String attachmentEntityName = "unit.test.TestService.EventItems.defaultSizeLimitedAttachments";
        CdsEntity entity = runtime.getCdsModel().findEntity(attachmentEntityName).orElseThrow();

        var attachment = Attachments.create();
        attachment.setId(UUID.randomUUID().toString());
        var content = mock(InputStream.class);
        attachment.setContent(content);

        when(target.entity()).thenReturn(entity);
        when(target.values()).thenReturn(attachment);
        when(target.keys()).thenReturn(Map.of(Attachments.ID, attachment.getId()));

        when(parameterInfo.getHeader("Content-Length")).thenReturn("399000000"); // 399MB

        var existingAttachments = List.<Attachments>of();

        // Act & Assert: No exception should be thrown as default is 400MB
        assertDoesNotThrow(
                () -> ModifyApplicationHandlerHelper.handleAttachmentForEntity(
                        existingAttachments,
                        eventFactory,
                        eventContext,
                        path,
                        content,
                        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));
    }

    @Test
    void malformedContentLengthHeader() {
        String attachmentEntityName = "unit.test.TestService.EventItems.sizeLimitedAttachments";
        CdsEntity entity = runtime.getCdsModel().findEntity(attachmentEntityName).orElseThrow();

        var attachment = Attachments.create();
        attachment.setId(UUID.randomUUID().toString());
        var content = mock(InputStream.class);
        attachment.setContent(content);

        when(target.entity()).thenReturn(entity);
        when(target.values()).thenReturn(attachment);
        when(target.keys()).thenReturn(Map.of(Attachments.ID, attachment.getId()));

        when(parameterInfo.getHeader("Content-Length")).thenReturn("invalid-number");

        var existingAttachments = List.<Attachments>of();

        // Act & Assert
        var exception = assertThrows(
                ServiceException.class,
                () -> ModifyApplicationHandlerHelper.handleAttachmentForEntity(
                        existingAttachments,
                        eventFactory,
                        eventContext,
                        path,
                        content,
                        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));

        assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.BAD_REQUEST);
    }
}
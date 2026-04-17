/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.CdsData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.InlineOnlyTable_;
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
import java.time.Instant;
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

    var existingAttachments = List.<Attachments>of();

    // Act & Assert
    var exception =
        assertThrows(
            ServiceException.class,
            () ->
                ModifyApplicationHandlerHelper.handleAttachmentForEntity(
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

    var existingAttachments = List.<Attachments>of();

    // Act & Assert
    var exception =
        assertThrows(
            ServiceException.class,
            () ->
                ModifyApplicationHandlerHelper.handleAttachmentForEntity(
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
        () ->
            ModifyApplicationHandlerHelper.handleAttachmentForEntity(
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
    var exception =
        assertThrows(
            ServiceException.class,
            () ->
                ModifyApplicationHandlerHelper.handleAttachmentForEntity(
                    existingAttachments,
                    eventFactory,
                    eventContext,
                    path,
                    content,
                    ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.BAD_REQUEST);
  }

  @Test
  void processInlineAttachmentRow_contentIsNotInputStream_treatedAsNull() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", "not-an-input-stream");

    var existing = Attachments.create();

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(null);

    ModifyApplicationHandlerHelper.processInlineAttachmentRow(
        row,
        "avatar",
        existing,
        inlineEntity,
        eventFactory,
        eventContext,
        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER);

    // content should be null since the value was not an InputStream
    verify(eventFactory).processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void processInlineAttachmentRow_contentLengthExceedsLimit_throwsException() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", mock(InputStream.class));

    var existing = Attachments.create();

    when(parameterInfo.getHeader("Content-Length")).thenReturn("999999999999");

    var exception =
        assertThrows(
            ServiceException.class,
            () ->
                ModifyApplicationHandlerHelper.processInlineAttachmentRow(
                    row, "avatar", existing, inlineEntity, eventFactory, eventContext, "10KB"));

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
  }

  @Test
  void processInlineAttachmentRow_invalidContentLengthHeader_throwsBadRequest() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", mock(InputStream.class));

    var existing = Attachments.create();

    when(parameterInfo.getHeader("Content-Length")).thenReturn("abc");

    var exception =
        assertThrows(
            ServiceException.class,
            () ->
                ModifyApplicationHandlerHelper.processInlineAttachmentRow(
                    row,
                    "avatar",
                    existing,
                    inlineEntity,
                    eventFactory,
                    eventContext,
                    ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));

    assertThat(exception.getErrorStatus()).isEqualTo(ErrorStatuses.BAD_REQUEST);
  }

  @Test
  void processInlineAttachmentRow_writesBackMetadata() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", mock(InputStream.class));

    var existing = Attachments.create();
    existing.setContentId("result-cid");
    existing.setStatus("Clean");
    existing.setScannedAt(Instant.parse("2025-01-01T00:00:00Z"));

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(null);

    ModifyApplicationHandlerHelper.processInlineAttachmentRow(
        row,
        "avatar",
        existing,
        inlineEntity,
        eventFactory,
        eventContext,
        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER);

    assertThat(row.get("avatar_contentId")).isEqualTo("result-cid");
    assertThat(row.get("avatar_status")).isEqualTo("Clean");
    assertThat(row.get("avatar_scannedAt")).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
  }

  @Test
  void processInlineAttachmentRow_doNothingEvent_doesNotWriteBackMetadata() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", mock(InputStream.class));
    row.put("avatar_contentId", "pre-existing-cid");

    // Existing attachment without contentId key - simulates doNothing result
    var existing = Attachments.create();

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(mock(InputStream.class));

    ModifyApplicationHandlerHelper.processInlineAttachmentRow(
        row,
        "avatar",
        existing,
        inlineEntity,
        eventFactory,
        eventContext,
        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER);

    // Pre-existing contentId should be preserved since existing doesn't contain CONTENT_ID key
    assertThat(row.get("avatar_contentId")).isEqualTo("pre-existing-cid");
  }

  @Test
  void processInlineAttachmentRow_limitExceeded_throwsTooLarge() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    byte[] largeContent = new byte[15000]; // 15KB
    row.put("avatar_content", new ByteArrayInputStream(largeContent));

    var existing = Attachments.create();

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              InputStream wrapped = invocation.getArgument(0);
              if (wrapped != null) {
                byte[] buffer = new byte[1024];
                while (wrapped.read(buffer) != -1) {}
              }
              return null;
            });

    var exception =
        assertThrows(
            ServiceException.class,
            () ->
                ModifyApplicationHandlerHelper.processInlineAttachmentRow(
                    row, "avatar", existing, inlineEntity, eventFactory, eventContext, "10KB"));

    assertThat(exception.getErrorStatus()).isEqualTo(ExtendedErrorStatuses.CONTENT_TOO_LARGE);
  }

  @Test
  void processInlineAttachmentRow_nonLimitException_rethrown() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", mock(InputStream.class));

    var existing = Attachments.create();

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("unexpected"));

    assertThrows(
        RuntimeException.class,
        () ->
            ModifyApplicationHandlerHelper.processInlineAttachmentRow(
                row,
                "avatar",
                existing,
                inlineEntity,
                eventFactory,
                eventContext,
                ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));
  }

  @Test
  void processInlineAttachmentRow_nullContent_noCountingInputStream() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", null);

    var existing = Attachments.create();

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(null);

    assertDoesNotThrow(
        () ->
            ModifyApplicationHandlerHelper.processInlineAttachmentRow(
                row,
                "avatar",
                existing,
                inlineEntity,
                eventFactory,
                eventContext,
                ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER));
  }

  @Test
  void handleAttachmentForEntities_withInlineAttachments_processesInline() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("avatar_content", mock(InputStream.class));

    when(eventFactory.processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(null);

    ModifyApplicationHandlerHelper.handleAttachmentForEntities(
        inlineEntity,
        List.of(row),
        List.of(),
        eventFactory,
        eventContext,
        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER);

    verify(eventFactory).processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void handleAttachmentForEntities_withoutInlineContent_skipsInline() {
    CdsEntity inlineEntity =
        runtime.getCdsModel().findEntity(InlineOnlyTable_.CDS_NAME).orElseThrow();

    var row = CdsData.create();
    row.put("ID", UUID.randomUUID().toString());
    row.put("title", "no inline");

    ModifyApplicationHandlerHelper.handleAttachmentForEntities(
        inlineEntity,
        List.of(row),
        List.of(),
        eventFactory,
        eventContext,
        ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER);

    // No inline processing since avatar_content is not in the row
    verify(eventFactory, never())
        .processInlineEvent(any(), any(), any(), any(), any(), any(), any(), any());
  }
}

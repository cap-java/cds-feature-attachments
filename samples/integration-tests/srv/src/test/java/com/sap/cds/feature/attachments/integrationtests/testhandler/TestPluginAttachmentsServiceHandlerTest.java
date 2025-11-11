/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestPluginAttachmentsServiceHandlerTest {

  private TestPluginAttachmentsServiceHandler cut;

  @BeforeEach
  void setup() {
    cut = new TestPluginAttachmentsServiceHandler();
    // Clear any previous test data
    cut.clearEventContext();
    cut.clearDocuments();
  }

  @Test
  void readIsWorking() {
    var context = AttachmentReadEventContext.create();
    context.setContentId("test");
    context.setData(MediaData.create());

    cut.readAttachment(context);

    assertThat(context.getData().getContent()).isNull();
  }

  @Test
  void readWithContentIsWorking() throws IOException {
    var createContext = AttachmentCreateEventContext.create();
    createContext.setData(MediaData.create());
    createContext
        .getData()
        .setContent(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
    cut.createAttachment(createContext);

    var context = AttachmentReadEventContext.create();
    context.setContentId(createContext.getContentId());
    context.setData(MediaData.create());

    cut.readAttachment(context);

    assertThat(context.getData().getContent().readAllBytes())
        .isEqualTo("test".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void dummyTestForDelete() {
    var context = AttachmentMarkAsDeletedEventContext.create();
    context.setContentId("test");

    assertDoesNotThrow(() -> cut.markAttachmentAsDeleted(context));
  }

  @Test
  void dummyTestForCreate() throws IOException {
    var context = AttachmentCreateEventContext.create();
    context.setData(MediaData.create());
    var stream = mock(InputStream.class);
    when(stream.readAllBytes()).thenReturn("test".getBytes(StandardCharsets.UTF_8));
    context.getData().setContent(stream);

    assertDoesNotThrow(() -> cut.createAttachment(context));
  }

  @Test
  void dummyTestForRestore() {
    var context = AttachmentRestoreEventContext.create();
    context.setRestoreTimestamp(Instant.now());

    assertDoesNotThrow(() -> cut.restoreAttachment(context));
  }

  @Test
  void testCreateAttachmentSetsContentIdAndStatus() throws IOException {
    var context = AttachmentCreateEventContext.create();
    context.setData(MediaData.create());
    context
        .getData()
        .setContent(new ByteArrayInputStream("test content".getBytes(StandardCharsets.UTF_8)));

    cut.createAttachment(context);

    assertNotNull(context.getContentId());
    assertThat(context.getData().getStatus()).isEqualTo(StatusCode.CLEAN);
  }

  @Test
  void testEventContextTracking() throws IOException {
    // Test create event tracking
    var createContext = AttachmentCreateEventContext.create();
    createContext.setData(MediaData.create());
    createContext
        .getData()
        .setContent(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
    cut.createAttachment(createContext);

    List<EventContextHolder> createEvents =
        cut.getEventContextForEvent(AttachmentService.EVENT_CREATE_ATTACHMENT);
    assertThat(createEvents).hasSize(1);
    assertThat(createEvents.get(0).event()).isEqualTo(AttachmentService.EVENT_CREATE_ATTACHMENT);

    // Test read event tracking
    var readContext = AttachmentReadEventContext.create();
    readContext.setContentId("test-id");
    readContext.setData(MediaData.create());
    cut.readAttachment(readContext);

    List<EventContextHolder> readEvents =
        cut.getEventContextForEvent(AttachmentService.EVENT_READ_ATTACHMENT);
    assertThat(readEvents).hasSize(1);
    assertThat(readEvents.get(0).event()).isEqualTo(AttachmentService.EVENT_READ_ATTACHMENT);

    // Test delete event tracking
    var deleteContext = AttachmentMarkAsDeletedEventContext.create();
    deleteContext.setContentId("test-id");
    cut.markAttachmentAsDeleted(deleteContext);

    List<EventContextHolder> deleteEvents =
        cut.getEventContextForEvent(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);
    assertThat(deleteEvents).hasSize(1);
    assertThat(deleteEvents.get(0).event())
        .isEqualTo(AttachmentService.EVENT_MARK_ATTACHMENT_AS_DELETED);

    // Test restore event tracking
    var restoreContext = AttachmentRestoreEventContext.create();
    restoreContext.setRestoreTimestamp(Instant.now());
    cut.restoreAttachment(restoreContext);

    List<EventContextHolder> restoreEvents =
        cut.getEventContextForEvent(AttachmentService.EVENT_RESTORE_ATTACHMENT);
    assertThat(restoreEvents).hasSize(1);
    assertThat(restoreEvents.get(0).event()).isEqualTo(AttachmentService.EVENT_RESTORE_ATTACHMENT);
  }

  @Test
  void testGetAllEventContext() throws IOException {
    // Create multiple events
    var createContext = AttachmentCreateEventContext.create();
    createContext.setData(MediaData.create());
    createContext
        .getData()
        .setContent(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
    cut.createAttachment(createContext);

    var readContext = AttachmentReadEventContext.create();
    readContext.setContentId("test-id");
    readContext.setData(MediaData.create());
    cut.readAttachment(readContext);

    List<EventContextHolder> allEvents = cut.getEventContext();
    assertThat(allEvents).hasSize(2);
  }

  @Test
  void testClearEventContext() throws IOException {
    // Add some events
    var context = AttachmentCreateEventContext.create();
    context.setData(MediaData.create());
    context.getData().setContent(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
    cut.createAttachment(context);

    assertThat(cut.getEventContext()).hasSize(1);

    // Clear and verify
    cut.clearEventContext();
    assertThat(cut.getEventContext()).isEmpty();
  }

  @Test
  void testReadWithNullContentId() {
    var context = AttachmentReadEventContext.create();
    context.setContentId(null);
    context.setData(MediaData.create());

    cut.readAttachment(context);

    assertThat(context.getData().getContent()).isNull();
  }

  @Test
  void testCreateAttachmentWithEmptyContent() throws IOException {
    var context = AttachmentCreateEventContext.create();
    context.setData(MediaData.create());
    context.getData().setContent(new ByteArrayInputStream(new byte[0]));

    cut.createAttachment(context);

    assertNotNull(context.getContentId());
    assertThat(context.getData().getStatus()).isEqualTo(StatusCode.CLEAN);
  }

  @Test
  void testMultipleCreateAndReadOperations() throws IOException {
    // Create first attachment
    var createContext1 = AttachmentCreateEventContext.create();
    createContext1.setData(MediaData.create());
    createContext1
        .getData()
        .setContent(new ByteArrayInputStream("content1".getBytes(StandardCharsets.UTF_8)));
    cut.createAttachment(createContext1);

    // Create second attachment
    var createContext2 = AttachmentCreateEventContext.create();
    createContext2.setData(MediaData.create());
    createContext2
        .getData()
        .setContent(new ByteArrayInputStream("content2".getBytes(StandardCharsets.UTF_8)));
    cut.createAttachment(createContext2);

    // Read first attachment
    var readContext1 = AttachmentReadEventContext.create();
    readContext1.setContentId(createContext1.getContentId());
    readContext1.setData(MediaData.create());
    cut.readAttachment(readContext1);

    // Read second attachment
    var readContext2 = AttachmentReadEventContext.create();
    readContext2.setContentId(createContext2.getContentId());
    readContext2.setData(MediaData.create());
    cut.readAttachment(readContext2);

    // Verify content
    assertThat(readContext1.getData().getContent().readAllBytes())
        .isEqualTo("content1".getBytes(StandardCharsets.UTF_8));
    assertThat(readContext2.getData().getContent().readAllBytes())
        .isEqualTo("content2".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void testRestoreWithSpecificTimestamp() {
    Instant timestamp = Instant.parse("2024-01-01T12:00:00Z");
    var context = AttachmentRestoreEventContext.create();
    context.setRestoreTimestamp(timestamp);

    cut.restoreAttachment(context);

    List<EventContextHolder> restoreEvents =
        cut.getEventContextForEvent(AttachmentService.EVENT_RESTORE_ATTACHMENT);
    assertThat(restoreEvents).hasSize(1);
    var restoredContext = (AttachmentRestoreEventContext) restoreEvents.get(0).context();
    assertThat(restoredContext.getRestoreTimestamp()).isEqualTo(timestamp);
  }
}

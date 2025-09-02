/*
 * © 2024-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.integrationtests.testhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestPluginAttachmentsServiceHandlerTest {

  private TestPluginAttachmentsServiceHandler cut;

  @BeforeEach
  void setup() {
    cut = new TestPluginAttachmentsServiceHandler();
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
}

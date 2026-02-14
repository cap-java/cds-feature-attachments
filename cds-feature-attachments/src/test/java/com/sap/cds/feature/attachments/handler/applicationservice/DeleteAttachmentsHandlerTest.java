/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Roots;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ModifyApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageReader;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.MarkAsDeletedAttachmentEvent;
import com.sap.cds.feature.attachments.handler.applicationservice.modifyevents.ModifyAttachmentEventFactory;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.common.AttachmentsReader;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.runtime.CdsRuntime;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteAttachmentsHandlerTest {

  private static CdsRuntime runtime;

  private ApplicationServiceAttachmentsHandler cut;
  private AttachmentsReader attachmentsReader;
  private MarkAsDeletedAttachmentEvent modifyAttachmentEvent;
  private CdsDeleteEventContext context;

  // Additional mocks required for ApplicationServiceAttachmentsHandler
  private ModifyAttachmentEventFactory eventFactory;
  private ThreadDataStorageReader storageReader;
  private AttachmentService attachmentService;
  private AttachmentStatusValidator statusValidator;
  private AsyncMalwareScanExecutor scanExecutor;
  private AttachmentService outboxedAttachmentService;

  @BeforeAll
  static void classSetup() {
    runtime = RuntimeHelper.runtime;
  }

  @BeforeEach
  void setup() {
    attachmentsReader = mock(AttachmentsReader.class);
    modifyAttachmentEvent = mock(MarkAsDeletedAttachmentEvent.class);
    eventFactory = mock(ModifyAttachmentEventFactory.class);
    storageReader = mock(ThreadDataStorageReader.class);
    attachmentService = mock(AttachmentService.class);
    statusValidator = mock(AttachmentStatusValidator.class);
    scanExecutor = mock(AsyncMalwareScanExecutor.class);
    outboxedAttachmentService = mock(AttachmentService.class);

    cut =
        new ApplicationServiceAttachmentsHandler(
            eventFactory,
            storageReader,
            ModifyApplicationHandlerHelper.DEFAULT_SIZE_WITH_SCANNER,
            attachmentService,
            statusValidator,
            scanExecutor,
            attachmentsReader,
            outboxedAttachmentService,
            modifyAttachmentEvent);

    context = mock(CdsDeleteEventContext.class);
  }

  @Test
  void noAttachmentDataServiceNotCalled() {
    var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(entity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());

    cut.processBeforeDelete(context);

    verifyNoInteractions(modifyAttachmentEvent);
  }

  @Test
  void attachmentDataExistsServiceIsCalled() {
    var entity = runtime.getCdsModel().findEntity(Attachment_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(entity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());
    var data = Attachments.create();
    data.setId("test");
    data.setContentId("test");
    var inputStream = mock(InputStream.class);
    data.setContent(inputStream);
    when(attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn()))
        .thenReturn(List.of(data));

    cut.processBeforeDelete(context);

    verify(modifyAttachmentEvent).processEvent(any(), eq(inputStream), eq(data), eq(context));
    assertThat(data.getContent()).isNull();
  }

  @Test
  void attachmentDataExistsAsExpandServiceIsCalled() {
    var rootEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME).orElseThrow();
    when(context.getTarget()).thenReturn(rootEntity);
    when(context.getModel()).thenReturn(runtime.getCdsModel());
    var inputStream = mock(InputStream.class);
    var attachment1 = buildAttachment("id1", inputStream);
    var attachment2 = buildAttachment(UUID.randomUUID().toString(), inputStream);
    var root = Roots.create();
    var items = Items.create();
    root.setItemTable(List.of(items));
    items.setAttachments(List.of(attachment1, attachment2));
    when(attachmentsReader.readAttachments(
            context.getModel(), context.getTarget(), context.getCqn()))
        .thenReturn(List.of(Attachments.of(root)));

    cut.processBeforeDelete(context);

    verify(modifyAttachmentEvent)
        .processEvent(
            any(Path.class), eq(inputStream), eq(Attachments.of(attachment1)), eq(context));
    verify(modifyAttachmentEvent)
        .processEvent(
            any(Path.class), eq(inputStream), eq(Attachments.of(attachment2)), eq(context));
    assertThat(attachment1.getContent()).isNull();
    assertThat(attachment2.getContent()).isNull();
  }

  @Test
  void classHasCorrectAnnotation() {
    var deleteHandlerAnnotation = cut.getClass().getAnnotation(ServiceName.class);

    assertThat(deleteHandlerAnnotation.type()).containsOnly(ApplicationService.class);
    assertThat(deleteHandlerAnnotation.value()).containsOnly("*");
  }

  @Test
  void methodHasCorrectAnnotations() throws NoSuchMethodException {
    var method = cut.getClass().getDeclaredMethod("processBeforeDelete", CdsDeleteEventContext.class);

    var deleteBeforeAnnotation = method.getAnnotation(Before.class);
    var deleteHandlerOrderAnnotation = method.getAnnotation(HandlerOrder.class);

    assertThat(deleteBeforeAnnotation.event()).containsOnly(CqnService.EVENT_DELETE);
    assertThat(deleteHandlerOrderAnnotation.value()).isEqualTo(HandlerOrder.LATE);
  }

  private Attachment buildAttachment(String id, InputStream inputStream) {
    var attachment = Attachment.create();
    attachment.setId(id);
    attachment.setContentId("doc_" + id);
    attachment.setContent(inputStream);
    return attachment;
  }
}

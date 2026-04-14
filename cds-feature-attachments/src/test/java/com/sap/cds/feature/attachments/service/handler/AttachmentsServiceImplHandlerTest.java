/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.service.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.generated.test.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.service.handler.transaction.EndTransactionMalwareScanProvider;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.changeset.ChangeSetListener;
import com.sap.cds.services.impl.changeset.ChangeSetContextImpl;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachmentsServiceImplHandlerTest {

  private DefaultAttachmentsServiceHandler cut;
  private EndTransactionMalwareScanProvider malwareScanProvider;

  @BeforeEach
  void setup() {
    malwareScanProvider = mock(EndTransactionMalwareScanProvider.class);
    cut = new DefaultAttachmentsServiceHandler(malwareScanProvider);
  }

  @AfterEach
  void tearDown() throws Exception {
    closeChangeSetContext();
  }

  @Test
  void createAttachmentsSetData() {
    var createContext = AttachmentCreateEventContext.create();
    var attachmentId = "test ID";
    createContext.setAttachmentIds(
        Map.of(Attachments.ID, attachmentId, "OtherId", "OtherID value"));
    createContext.setData(MediaData.create());
    createContext.setAttachmentEntity(mock(CdsEntity.class));
    ChangeSetContextImpl.open(false);

    cut.createAttachment(createContext);

    assertThat(createContext.isCompleted()).isTrue();
    assertThat(createContext.getContentId()).isEqualTo(attachmentId);
    assertThat(createContext.getIsInternalStored()).isTrue();
    assertThat(createContext.getData().getStatus()).isEqualTo(StatusCode.SCANNING);
  }

  @Test
  void deleteAttachmentSetData() {
    var deleteContext = AttachmentMarkAsDeletedEventContext.create();

    cut.markAttachmentAsDeleted(deleteContext);

    assertThat(deleteContext.isCompleted()).isTrue();
  }

  @Test
  void restoreAttachmentAttachmentSetData() {
    var restoreContext = AttachmentRestoreEventContext.create();

    cut.restoreAttachment(restoreContext);

    assertThat(restoreContext.isCompleted()).isTrue();
  }

  @Test
  void readAttachmentSetData() {
    var readContext = AttachmentReadEventContext.create();

    cut.readAttachment(readContext);

    assertThat(readContext.isCompleted()).isTrue();
  }

  @Test
  void malwareScannerRegisteredForEndOfTransaction() {
    var listener = mock(ChangeSetListener.class);
    var entity = mock(CdsEntity.class);
    when(malwareScanProvider.getChangeSetListener(entity, "contentId")).thenReturn(listener);
    var createContext = AttachmentCreateEventContext.create();
    createContext.setAttachmentIds(Map.of(Attachments.ID, "contentId"));
    createContext.setData(MediaData.create());
    createContext.setAttachmentEntity(entity);
    ChangeSetContextImpl.open(false);

    cut.createAttachment(createContext);
    cut.afterCreateAttachment(createContext);

    verify(malwareScanProvider).getChangeSetListener(entity, "contentId");
  }

  @Test
  void createAttachment_emptyAttachmentIds_handlesGracefully() {
    var createContext = AttachmentCreateEventContext.create();
    createContext.setAttachmentIds(Collections.emptyMap());
    createContext.setData(MediaData.create());
    createContext.setAttachmentEntity(mock(CdsEntity.class));
    ChangeSetContextImpl.open(false);

    cut.createAttachment(createContext);

    assertThat(createContext.getContentId())
        .as("contentId should be null when Attachments.ID key is missing from attachmentIds")
        .isNull();
    assertThat(createContext.isCompleted()).isTrue();
  }

  @Test
  void afterCreateAttachment_noChangeSetContext_throws() {
    var entity = mock(CdsEntity.class);
    when(malwareScanProvider.getChangeSetListener(any(), any()))
        .thenReturn(mock(ChangeSetListener.class));
    var createContext = AttachmentCreateEventContext.create();
    createContext.setAttachmentIds(Map.of(Attachments.ID, "some-id"));
    createContext.setData(MediaData.create());
    createContext.setAttachmentEntity(entity);

    cut.createAttachment(createContext);

    assertThatThrownBy(() -> cut.afterCreateAttachment(createContext))
        .isInstanceOf(NullPointerException.class);
  }

  private void closeChangeSetContext() throws Exception {
    var context = ChangeSetContextImpl.getCurrent();
    if (Objects.nonNull(context)) {
      try {
        context.close();
      } catch (RuntimeException ignored) {
        // ignore
      }
    }
  }
}

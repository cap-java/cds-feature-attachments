/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.services.changeset.ChangeSetContext;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.ChangeSetContextRunner;
import com.sap.cds.services.runtime.RequestContextRunner;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreationChangeSetListenerTest {

  private CreationChangeSetListener cut;
  private String contentId;
  private CdsRuntime cdsRuntime;
  private AttachmentService outboxedAttachmentService;
  private RequestContextRunner requestContextRunner;
  private ChangeSetContextRunner changeSetContextRunner;
  private ArgumentCaptor<Consumer<RequestContext>> requestContextCaptor;
  private ArgumentCaptor<Consumer<ChangeSetContext>> changeSetContextCaptor;
  private UserInfo userInfo;

  @BeforeEach
  void setup() {
    contentId = "contentId";
    cdsRuntime = mock(CdsRuntime.class);
    outboxedAttachmentService = mock(AttachmentService.class);
    cut = new CreationChangeSetListener(contentId, cdsRuntime, outboxedAttachmentService);

    requestContextRunner = mock(RequestContextRunner.class);
    changeSetContextRunner = mock(ChangeSetContextRunner.class);
    when(cdsRuntime.requestContext()).thenReturn(requestContextRunner);
    when(cdsRuntime.changeSetContext()).thenReturn(changeSetContextRunner);
    requestContextCaptor = ArgumentCaptor.forClass(Consumer.class);
    changeSetContextCaptor = ArgumentCaptor.forClass(Consumer.class);
    userInfo = mock(UserInfo.class);
  }

  @Test
  void onlyExecutedIfTransactionHasErrors() {
    cut.afterClose(false);

    // Verify requestContext().run() is called
    verify(requestContextRunner).run(requestContextCaptor.capture());
    var requestContextConsumer = requestContextCaptor.getValue();
    var requestContextMock = mock(RequestContext.class);
    when(requestContextMock.getUserInfo()).thenReturn(userInfo);

    // Execute the request context consumer, which should trigger changeSetContext().run()
    requestContextConsumer.accept(requestContextMock);

    // Verify changeSetContext().run() is called for independent transaction
    verify(changeSetContextRunner).run(changeSetContextCaptor.capture());
    var changeSetContextConsumer = changeSetContextCaptor.getValue();
    var changeSetContextMock = mock(ChangeSetContext.class);

    // Execute the changeset context consumer
    changeSetContextConsumer.accept(changeSetContextMock);

    // Verify markAttachmentAsDeleted is called with correct parameters
    var deletionInputCaptor = ArgumentCaptor.forClass(MarkAsDeletedInput.class);
    verify(outboxedAttachmentService).markAttachmentAsDeleted(deletionInputCaptor.capture());
    assertThat(deletionInputCaptor.getValue().contentId()).isEqualTo(contentId);
    assertThat(deletionInputCaptor.getValue().userInfo()).isEqualTo(userInfo);
  }

  @Test
  void noExecutionIfTransactionCompleted() {
    cut.afterClose(true);

    verifyNoInteractions(cdsRuntime, outboxedAttachmentService, requestContextRunner);
  }
}

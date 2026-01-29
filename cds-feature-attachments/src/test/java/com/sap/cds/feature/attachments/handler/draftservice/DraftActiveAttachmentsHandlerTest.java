/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.draftservice;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadDataStorageSetter;
import com.sap.cds.feature.attachments.handler.common.AttachmentCountValidator;
import com.sap.cds.services.draft.DraftSaveEventContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DraftActiveAttachmentsHandlerTest {

  private DraftActiveAttachmentsHandler cut;
  private ThreadDataStorageSetter threadLocalSetter;
  private AttachmentCountValidator validator;
  private ArgumentCaptor<Runnable> runnableCaptor;

  @BeforeEach
  void setup() {
    threadLocalSetter = mock(ThreadDataStorageSetter.class);
    validator = mock(AttachmentCountValidator.class);
    cut = new DraftActiveAttachmentsHandler(threadLocalSetter, validator);

    runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
  }

  @Test
  void setterCalled() {
    var context = mock(DraftSaveEventContext.class);

    cut.processDraftSave(context);

    verify(threadLocalSetter).set(eq(true), runnableCaptor.capture());
    verifyNoInteractions(context);
    runnableCaptor.getValue().run();
    verify(context).proceed();
  }
}

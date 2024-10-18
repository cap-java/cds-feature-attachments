package com.sap.cds.feature.attachments.handler.draftservice;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.ThreadLocalDataStorage;
import com.sap.cds.services.draft.DraftSaveEventContext;

class DraftActiveAttachmentsHandlerTest {

	private DraftActiveAttachmentsHandler cut;
	private ThreadLocalDataStorage threadLocalSetter;
	private ArgumentCaptor<Runnable> runnableCaptor;

	@BeforeEach
	void setup() {
		threadLocalSetter = mock(ThreadLocalDataStorage.class);
		cut = new DraftActiveAttachmentsHandler(threadLocalSetter);

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

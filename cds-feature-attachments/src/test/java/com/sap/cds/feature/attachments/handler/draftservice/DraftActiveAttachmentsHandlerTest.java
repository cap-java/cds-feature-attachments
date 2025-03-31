package com.sap.cds.feature.attachments.handler.draftservice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.services.draft.DraftSaveEventContext;

class DraftActiveAttachmentsHandlerTest {

	private DraftActiveAttachmentsHandler cut;
	private ArgumentCaptor<Runnable> runnableCaptor;

	@BeforeEach
	void setup() {
		cut = new DraftActiveAttachmentsHandler();

		runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
	}

	@Test
	void setterCalled() {
		var context = mock(DraftSaveEventContext.class);

		cut.processDraftSave(context);

		verifyNoInteractions(context);
		runnableCaptor.getValue().run();
		verify(context).proceed();
	}

}

package com.sap.cds.feature.attachments.handler.applicationservice.processor.transaction;

import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.RequestContextRunner;

class CreationChangeSetListenerTest {

	private CreationChangeSetListener cut;
	private String documentId;
	private CdsRuntime cdsRuntime;
	private AttachmentService outboxedAttachmentService;
	private RequestContextRunner requestContextRunner;
	private ArgumentCaptor<Consumer<RequestContext>> requestContextCaptor;

	@BeforeEach
	void setup() {
		documentId = "documentId";
		cdsRuntime = mock(CdsRuntime.class);
		outboxedAttachmentService = mock(AttachmentService.class);
		cut = new CreationChangeSetListener(documentId, cdsRuntime, outboxedAttachmentService);

		requestContextRunner = mock(RequestContextRunner.class);
		when(cdsRuntime.requestContext()).thenReturn(requestContextRunner);
		requestContextCaptor = ArgumentCaptor.forClass(Consumer.class);
	}

	@Test
	void onlyExecutedIfTransactionHasErrors() {
		cut.afterClose(false);

		verify(requestContextRunner).run(requestContextCaptor.capture());
		var requestContext = requestContextCaptor.getValue();
		requestContext.accept(mock(RequestContext.class));
		verify(outboxedAttachmentService).markAttachmentAsDeleted(documentId);
	}

	@Test
	void noExecutionIfTransactionCompleted() {
		cut.afterClose(true);

		verifyNoInteractions(cdsRuntime, outboxedAttachmentService, requestContextRunner);
	}

}

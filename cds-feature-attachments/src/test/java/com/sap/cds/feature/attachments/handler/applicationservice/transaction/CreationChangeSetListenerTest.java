/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.services.request.RequestContext;
import com.sap.cds.services.request.UserInfo;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.runtime.RequestContextRunner;

class CreationChangeSetListenerTest {

	private CreationChangeSetListener cut;
	private String contentId;
	private CdsRuntime cdsRuntime;
	private AttachmentService outboxedAttachmentService;
	private RequestContextRunner requestContextRunner;
	private ArgumentCaptor<Consumer<RequestContext>> requestContextCaptor;
	private UserInfo userInfo;

	@BeforeEach
	void setup() {
		contentId = "contentId";
		cdsRuntime = mock(CdsRuntime.class);
		outboxedAttachmentService = mock(AttachmentService.class);
		cut = new CreationChangeSetListener(contentId, cdsRuntime, outboxedAttachmentService);

		requestContextRunner = mock(RequestContextRunner.class);
		when(cdsRuntime.requestContext()).thenReturn(requestContextRunner);
		requestContextCaptor = ArgumentCaptor.forClass(Consumer.class);
		userInfo = mock(UserInfo.class);
	}

	@Test
	void onlyExecutedIfTransactionHasErrors() {
		cut.afterClose(false);

		verify(requestContextRunner).run(requestContextCaptor.capture());
		var requestContext = requestContextCaptor.getValue();
		var requestContextMock = mock(RequestContext.class);
		when(requestContextMock.getUserInfo()).thenReturn(userInfo);
		requestContext.accept(requestContextMock);
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

package com.sap.cds.feature.attachments.handler.restore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.restoreattachments.RestoreAttachmentsContext;
import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.restoreattachments.RestoreAttachments_;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

class RestoreAttachmentsHandlerTest {

	private RestoreAttachmentsHandler cut;
	private AttachmentService attachmentService;

	@BeforeEach
	void setup() {
		attachmentService = mock(AttachmentService.class);
		cut = new RestoreAttachmentsHandler(attachmentService);
	}

	@Test
	void attachmentServiceCalled() {
		var timestamp = Instant.now();
		var context = RestoreAttachmentsContext.create();
		context.setRestoreTimestamp(timestamp);

		cut.restoreAttachments(context);

		assertThat(context.isCompleted()).isTrue();
		verify(attachmentService).restore(timestamp);
	}

	@Test
	void methodHasCorrectAnnotation() throws NoSuchMethodException {
		var methodAnnotation = cut.getClass().getMethod("restoreAttachments", RestoreAttachmentsContext.class).getAnnotation(
				On.class);

		assertThat(methodAnnotation.event()).contains(RestoreAttachmentsContext.CDS_NAME);
	}

	@Test
	void classHasCorrectAnnotation() {
		var classAnnotation = cut.getClass().getAnnotation(ServiceName.class);

		assertThat(classAnnotation.value()).contains(RestoreAttachments_.CDS_NAME);
	}

}

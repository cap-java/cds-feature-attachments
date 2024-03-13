package com.sap.cds.feature.attachments.integrationtests.testhandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentDeleteEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentUpdateEventContext;

class TestPluginAttachmentsServiceHandlerTest {

	private TestPluginAttachmentsServiceHandler cut;

	@BeforeEach
	void setup() {
		cut = new TestPluginAttachmentsServiceHandler();
	}

	@Test
	void dummyTestForRead() {
		var context = AttachmentReadEventContext.create();
		context.setDocumentId("test");
		context.setData(MediaData.create());

		assertDoesNotThrow(() -> cut.readAttachment(context));
	}

	@Test
	void dummyTestForDelete() {
		var context = AttachmentDeleteEventContext.create();
		context.setDocumentId("test");

		assertDoesNotThrow(() -> cut.deleteAttachment(context));
	}

	@Test
	void dummyTestForUpdate() throws IOException {
		var context = AttachmentUpdateEventContext.create();
		context.setData(MediaData.create());
		var stream = mock(InputStream.class);
		when(stream.readAllBytes()).thenReturn("test".getBytes(StandardCharsets.UTF_8));
		context.getData().setContent(stream);
		context.setDocumentId("test");

		assertDoesNotThrow(() -> cut.updateAttachment(context));
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

}

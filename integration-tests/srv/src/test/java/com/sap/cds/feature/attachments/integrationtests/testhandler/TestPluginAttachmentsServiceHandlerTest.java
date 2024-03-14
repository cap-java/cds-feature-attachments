package com.sap.cds.feature.attachments.integrationtests.testhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
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
	void readIsWorking() {
		var context = AttachmentReadEventContext.create();
		context.setDocumentId("test");
		context.setData(MediaData.create());

		cut.readAttachment(context);

		assertThat(context.getData().getContent()).isNull();
	}

	@Test
	void readWithContentIsWorking() throws IOException {
		var createContext = AttachmentCreateEventContext.create();
		createContext.setData(MediaData.create());
		createContext.getData().setContent(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
		cut.createAttachment(createContext);

		var context = AttachmentReadEventContext.create();
		context.setDocumentId(createContext.getDocumentId());
		context.setData(MediaData.create());

		cut.readAttachment(context);

		assertThat(context.getData().getContent().readAllBytes()).isEqualTo("test".getBytes(StandardCharsets.UTF_8));
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
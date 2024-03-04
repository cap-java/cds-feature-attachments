package com.sap.cds.feature.attachments.handler.processor.applicationevents.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.service.AttachmentService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class LazyProxyInputStreamTest {

	private LazyProxyInputStream cut;
	private InputStream inputStream;
	private AttachmentService attachmentService;

	@BeforeEach
	void setup() {
		inputStream = mock(InputStream.class);
		attachmentService = mock(AttachmentService.class);
		when(attachmentService.readAttachment(any())).thenReturn(inputStream);
		cut = new LazyProxyInputStream(() -> attachmentService.readAttachment(any()));
	}

	@Test
	void noMethodCallNoStreamAccess() {
		verifyNoInteractions(attachmentService);
	}

	@Test
	void simpleReadIsForwarded() throws IOException {
		when(inputStream.read()).thenReturn(12);

		var result = cut.read();

		verify(inputStream).read();
		assertThat(result).isEqualTo(12);
	}

	@Test
	@SuppressFBWarnings("RR_NOT_CHECKED")
	void readWithBytesIsForwarded() throws IOException {
		var bytes = "test".getBytes(StandardCharsets.UTF_8);
		when(inputStream.read(bytes)).thenReturn(24);

		var result = cut.read(bytes);

		verify(inputStream).read(bytes);
		assertThat(result).isEqualTo(24);
	}

	@Test
	@SuppressFBWarnings("RR_NOT_CHECKED")
	void readWithBytesAndParametersIsForwarded() throws IOException {
		var bytes = "test".getBytes(StandardCharsets.UTF_8);
		when(inputStream.read(bytes, 1, 2)).thenReturn(36);

		var result = cut.read(bytes, 1, 2);

		verify(inputStream).read(bytes, 1, 2);
		assertThat(result).isEqualTo(36);
	}

	@Test
	void supplierOnlyCalledOnce() throws IOException {
		when(inputStream.read()).thenReturn(48).thenReturn(60);

		var result1 = cut.read();
		var result2 = cut.read();

		verify(inputStream, times(2)).read();
		verify(attachmentService).readAttachment(any());
		assertThat(result1).isEqualTo(48);
		assertThat(result2).isEqualTo(60);
	}

	@Test
	void closeDoesNotCallSupplier() throws IOException {
		cut.close();

		verifyNoInteractions(inputStream);
		verifyNoInteractions(attachmentService);
	}

	@Test
	void closeCallsInputStream() throws IOException {
		cut.read();
		cut.close();

		verify(inputStream, times(1)).read();
		verify(inputStream, times(1)).close();
		verify(attachmentService).readAttachment(any());
	}

}

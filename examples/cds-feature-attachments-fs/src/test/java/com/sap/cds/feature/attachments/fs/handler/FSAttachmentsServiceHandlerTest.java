/*
 * © 2025-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.fs.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.request.UserInfo;

class FSAttachmentsServiceHandlerTest {

	private static final String TEST_CONTENT = "Hello World !!";

	private static FSAttachmentsServiceHandler handler;

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path rootFolder;

	private static CdsEntity entity;

	@BeforeAll
	static void setUpBeforeClass() throws IOException {
		handler = new FSAttachmentsServiceHandler(rootFolder);
		entity = mock(CdsEntity.class);
		when(entity.getQualifiedName()).thenReturn("test.Attachments");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "t0" })
	void testCreateAttachment(String tenant) throws IOException {
		String contentId = UUID.randomUUID().toString();
		AttachmentCreateEventContext createContext = createAttachment(tenant, contentId, TEST_CONTENT);

		assertEquals(contentId, createContext.getContentId());
		Path file = resolveContentPath(tenant, contentId);
		assertTrue(Files.exists(file));
		assertTrue(createContext.isCompleted());
		assertFalse(createContext.getIsInternalStored());

		String content = Files.readString(file);
		assertEquals(TEST_CONTENT, content);

	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "t0" })
	void testReadAttachment(String tenant) throws IOException {
		String contentId = UUID.randomUUID().toString();
		createAttachment(tenant, contentId, TEST_CONTENT);

		AttachmentReadEventContext context = spy(AttachmentReadEventContext.create());
		context.setContentId(contentId);
		context.setData(MediaData.create());
		doReturn(getUserInfoMock(tenant)).when(context).getUserInfo();

		handler.readAttachment(context);

		String content = IOUtils.toString(context.getData().getContent(), UTF_8);
		assertEquals(TEST_CONTENT, content);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "t0" })
	void testMarkAttachmentAsDeleted(String tenant) throws IOException {
		String contentId = UUID.randomUUID().toString();
		createAttachment(tenant, contentId, TEST_CONTENT);

		AttachmentMarkAsDeletedEventContext context = spy(AttachmentMarkAsDeletedEventContext.create());
		doReturn(getUserInfoMock(tenant)).when(context).getUserInfo();
		context.setContentId(contentId);

		Path filePath = resolveContentPath(tenant, contentId);
		Path deletedPath = resolveDeletedContentPath(tenant, contentId);
		assertTrue(Files.exists(filePath));
		assertFalse(Files.exists(deletedPath));

		handler.markAttachmentAsDeleted(context);

		assertFalse(Files.exists(filePath));
		assertTrue(Files.exists(deletedPath));
		assertTrue(context.isCompleted());
	}

	@Test
	void testRestoreAttachment() {
	}

	private static AttachmentCreateEventContext createAttachment(String tenant, String id, String content)
			throws IOException {
		AttachmentCreateEventContext createContext = spy(AttachmentCreateEventContext.create());
		createContext.setAttachmentEntity(entity);
		doReturn(getUserInfoMock(tenant)).when(createContext).getUserInfo();
		assertFalse(createContext.isCompleted());
		assertNull(createContext.getIsInternalStored());

		Map<String, Object> keys = Map.of(Attachments.ID, id);
		createContext.setAttachmentIds(keys);
		try (InputStream testStream = new ByteArrayInputStream(content.getBytes(UTF_8))) {
			MediaData mediaData = MediaData.create();
			mediaData.setContent(testStream);
			createContext.setData(mediaData);

			handler.createAttachment(createContext);
			return createContext;
		}
	}

	private static UserInfo getUserInfoMock(String tenant) {
		UserInfo userInfo = mock(UserInfo.class);
		when(userInfo.getTenant()).thenReturn(tenant);
		return userInfo;
	}

	private static Path resolveDeletedContentPath(String tenant, String contentId) {
		return rootFolder
				.resolve("%s/deleted/%s/content.bin".formatted(tenant == null ? "default" : tenant, contentId));
	}

	private static Path resolveContentPath(String tenant, String contentId) {
		return rootFolder.resolve("%s/%s/content.bin".formatted(tenant == null ? "default" : tenant, contentId));
	}

}

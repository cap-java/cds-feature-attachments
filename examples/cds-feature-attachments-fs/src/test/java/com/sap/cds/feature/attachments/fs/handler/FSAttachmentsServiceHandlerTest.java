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
import org.junit.jupiter.api.BeforeEach;
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

	private String parentId;

	private String attachmentId;

	@BeforeEach
	void setUpBeforeEach() {
		parentId = UUID.randomUUID().toString();
		attachmentId = UUID.randomUUID().toString();
	}

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
		AttachmentCreateEventContext createContext = createAttachment(tenant, parentId, attachmentId, TEST_CONTENT);

		assertEquals("%s:%s".formatted(parentId, attachmentId), createContext.getContentId());
		Path file = resolveContentPath(tenant, parentId, attachmentId);
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
		var ctx = createAttachment(tenant, parentId, attachmentId, TEST_CONTENT);

		AttachmentReadEventContext context = spy(AttachmentReadEventContext.create());
		context.setContentId(ctx.getContentId());
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
		var ctx = createAttachment(tenant, parentId, attachmentId, TEST_CONTENT);

		AttachmentMarkAsDeletedEventContext context = spy(AttachmentMarkAsDeletedEventContext.create());
		doReturn(getUserInfoMock(tenant)).when(context).getUserInfo();
		context.setContentId(ctx.getContentId());

		Path filePath = resolveContentPath(tenant, parentId, attachmentId);
		Path deletedPath = resolveDeletedContentPath(tenant, attachmentId);
		assertTrue(Files.exists(filePath));
		assertFalse(Files.exists(deletedPath));

		handler.markAttachmentAsDeleted(context);

		assertFalse(Files.exists(filePath));
		assertTrue(Files.exists(deletedPath));
		assertTrue(context.isCompleted());
	}

	private static AttachmentCreateEventContext createAttachment(String tenant, String parentId, String attachmentId,
			String content) throws IOException {
		AttachmentCreateEventContext createContext = spy(AttachmentCreateEventContext.create());
		createContext.setAttachmentEntity(entity);
		doReturn(getUserInfoMock(tenant)).when(createContext).getUserInfo();
		assertFalse(createContext.isCompleted());
		assertNull(createContext.getIsInternalStored());

		Map<String, Object> keys = Map.of(Attachments.ID, attachmentId);
		Map<String, Object> parentKeys = Map.of("up__ID", parentId);
		createContext.setAttachmentIds(keys);
		createContext.setParentIds(parentKeys);
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

	private static Path resolveDeletedContentPath(String tenant, String attachmentId) {
		return rootFolder
				.resolve("%s/deleted/%s/content.bin".formatted(tenant == null ? "default" : tenant, attachmentId));
	}

	private static Path resolveContentPath(String tenant, String parentId, String attachmentId) {
		return rootFolder
				.resolve("%s/%s/%s/content.bin".formatted(tenant == null ? "default" : tenant, parentId, attachmentId));
	}
}

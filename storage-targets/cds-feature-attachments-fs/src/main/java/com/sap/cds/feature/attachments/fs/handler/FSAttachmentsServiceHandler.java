/*
 * © 2025-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.fs.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
 * This class is an event handler that is called when an attachment is created, marked as deleted, restored or read.
 */
@ServiceName(value = "*", type = AttachmentService.class)
public class FSAttachmentsServiceHandler implements EventHandler {

	@VisibleForTesting
	static final String PATTERN_CONTENT_BIN = "%s/%s/%s/content.bin";

	@VisibleForTesting
	static final String PATTERN_DELETED_FOLDER = "%s/deleted/%s";

	private static final Logger logger = LoggerFactory.getLogger(FSAttachmentsServiceHandler.class);

	private final Path rootFolder;

	/**
	 * Creates a new FSAttachmentsServiceHandler with the given root folder.
	 * 
	 * @param rootFolder the root folder where the attachments are stored
	 * @throws IOException if the root folder cannot be created
	 */
	public FSAttachmentsServiceHandler(Path rootFolder) throws IOException {
		this.rootFolder = rootFolder;
		if (!Files.exists(this.rootFolder)) {
			Files.createDirectories(this.rootFolder);
		}
	}

	@On
	void createAttachment(AttachmentCreateEventContext context) throws IOException {
		logger.info("FS Attachment Service handler called for creating attachment for entity name: {}",
				context.getAttachmentEntity().getQualifiedName());

		String attachmentId = (String) context.getAttachmentIds().get(Attachments.ID);
		String parentId = (String) context.getParentIds().get("up__ID");

		MediaData data = context.getData();
		data.setStatus(StatusCode.CLEAN);

		try (InputStream input = data.getContent()) {
			Path contentPath = getContentPath(context, parentId, attachmentId);
			Files.createDirectories(contentPath.getParent());
			Files.copy(input, contentPath);

			context.setIsInternalStored(false);
			context.setContentId(getContentId(parentId, attachmentId));
			context.setCompleted();
		}
	}

	@On
	void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws IOException {
		logger.info("Marking attachment as deleted with document id: {}", context.getContentId());

		Path contentPath = getContentPath(context, context.getContentId());
		Path parent = contentPath.getParent();
		Path destPath = getDeletedFolder(context, context.getContentId()).resolve(parent.getFileName());

		FileUtils.moveDirectory(parent.toFile(), destPath.toFile());
		context.setCompleted();
	}

	@On
	void restoreAttachment(AttachmentRestoreEventContext context) {
		logger.info("FS Attachment Service handler called for restoring attachment for timestamp: {}",
				context.getRestoreTimestamp());

		// nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

	@On
	void readAttachment(AttachmentReadEventContext context) throws IOException {
		logger.info("FS Attachment Service handler called for reading attachment with document id: {}",
				context.getContentId());
		InputStream fileInputStream = Files.newInputStream(getContentPath(context, context.getContentId()));
		context.getData().setContent(fileInputStream);
		context.setCompleted();
	}

	private Path getContentPath(EventContext context, String contentId) {
		ParentAttachmentIds parentAttachmentId = getParentAttachmentId(contentId);
		return getContentPath(context, parentAttachmentId.parentId(), parentAttachmentId.attachmentId());
	}

	private Path getContentPath(EventContext context, String parentId, String attachmentId) {
		return rootFolder.resolve(PATTERN_CONTENT_BIN.formatted(getTenant(context), parentId, attachmentId));
	}

	private Path getDeletedFolder(EventContext context, String contentId) {
		ParentAttachmentIds parentAttachmentId = getParentAttachmentId(contentId);
		return rootFolder.resolve(PATTERN_DELETED_FOLDER.formatted(getTenant(context), parentAttachmentId.parentId()));
	}

	private static String getTenant(EventContext context) {
		String tenant = context.getUserInfo().getTenant();
		if (tenant == null) {
			tenant = "default";
		}
		return tenant;
	}

	private static String getContentId(String parentId, String attachmentId) {
		return "%s:%s".formatted(parentId, attachmentId);
	}

	private static ParentAttachmentIds getParentAttachmentId(String contentId) {
		String[] parts = contentId.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid content ID format: %s".formatted(contentId));
		}
		return new ParentAttachmentIds(parts[0], parts[1]);
	}

	record ParentAttachmentIds(String parentId, String attachmentId) {
	}
}

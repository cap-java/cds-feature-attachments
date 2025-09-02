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

		String contentId = (String) context.getAttachmentIds().get(Attachments.ID);

		MediaData data = context.getData();
		data.setStatus(StatusCode.CLEAN);

		try (InputStream input = data.getContent()) {
			Path contentPath = getContentPath(context, contentId);
			Files.createDirectories(contentPath.getParent());
			Files.copy(input, contentPath);

			context.setIsInternalStored(false);
			context.setContentId(contentId);
			context.setCompleted();
		}
	}

	@On
	void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws IOException {
		logger.info("Marking attachment as deleted with document id: {}", context.getContentId());

		Path contenPath = getContentPath(context, context.getContentId());
		Path parent = contenPath.getParent();
		Path destPath = getDeletedFolder(context).resolve(parent.getFileName());

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
		return this.rootFolder.resolve("%s/%s/content.bin".formatted(getTenant(context), contentId));
	}

	private Path getDeletedFolder(EventContext context) {
		return this.rootFolder.resolve("%s/deleted".formatted(getTenant(context)));
	}

	private static String getTenant(EventContext context) {
		String tenant = context.getUserInfo().getTenant();
		if (tenant == null) {
			tenant = "default";
		}
		return tenant;
	}

}

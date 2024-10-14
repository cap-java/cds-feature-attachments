/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
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

	private final Path deletedFolder;

	public FSAttachmentsServiceHandler(Path rootFolder) throws IOException {
		this.rootFolder = rootFolder;
		this.deletedFolder = rootFolder.resolve("deleted");
		if (!Files.exists(this.rootFolder)) {
			Files.createDirectories(this.rootFolder);
		}
		if (!Files.exists(this.deletedFolder)) {
			Files.createDirectories(this.deletedFolder);
		}
	}

	@On
	public void createAttachment(AttachmentCreateEventContext context) throws IOException {
		logger.info("FS Attachment Service handler called for creating attachment for entity name: {}",
				context.getAttachmentEntity().getQualifiedName());
		String contentId = (String) context.getAttachmentIds().get(Attachments.ID);

		MediaData data = context.getData();
		data.setStatus(StatusCode.CLEAN);

		try (InputStream input = data.getContent()) {
			Path contentPath = getContentPath(contentId);
			Files.createDirectories(contentPath.getParent());
			Files.copy(input, contentPath);

			context.setIsInternalStored(false);
			context.setContentId(contentId);
			context.setCompleted();
		}
	}

	@On
	public void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws IOException {
		logger.info("Marking attachment as deleted with document id: {}", context.getContentId());

		Path contenPath = getContentPath(context.getContentId());
		Path parent = contenPath.getParent();
		FileUtils.moveDirectory(parent.toFile(), this.deletedFolder.resolve(parent.getFileName()).toFile());
		context.setCompleted();
	}

	@On
	public void restoreAttachment(AttachmentRestoreEventContext context) {
		logger.info("FS Attachment Service handler called for restoring attachment for timestamp: {}",
				context.getRestoreTimestamp());

		// nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

	@On
	public void readAttachment(AttachmentReadEventContext context) throws IOException {
		logger.info("FS Attachment Service handler called for reading attachment with document id: {}",
				context.getContentId());

		InputStream fileInputStream = Files.newInputStream(getContentPath(context.getContentId()));
		context.getData().setContent(fileInputStream);
		context.setCompleted();
	}

	private Path getContentPath(String contentId) {
		return this.rootFolder.resolve("%s/content.bin".formatted(contentId));
	}

}

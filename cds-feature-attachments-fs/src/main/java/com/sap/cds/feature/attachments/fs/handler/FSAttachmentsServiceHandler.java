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
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

/**
 * The class {@link FSAttachmentsServiceHandler} is an event handler that is called when an attachment is created,
 * marked as deleted, restored or read.
 * <p>
 * As the documents and content is stored in the database with this handler the handler sets the isInternalStored flag
 * to true in the create-context. Without this flag the content would be deleted in the database.
 */
@ServiceName(value = "*", type = AttachmentService.class)
public class FSAttachmentsServiceHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(FSAttachmentsServiceHandler.class);

	private final Path rootFolder;

	public FSAttachmentsServiceHandler() throws IOException {
		Path tmpPath = FileUtils.getTempDirectory().toPath();
		this.rootFolder = tmpPath.resolve(this.getClass().getCanonicalName());
		Files.createDirectories(this.rootFolder);
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

			Path metaDataPath = getMetaDataPath(contentId);
			Files.writeString(metaDataPath, data.toJson());

			context.setIsInternalStored(false);
			context.setContentId(contentId);
			context.setCompleted();
		}
	}

	@On
	public void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws IOException {
		logger.info("Marking attachment as deleted with document id: {}", context.getContentId());

		Path contenPath = getContentPath(context.getContentId());
		Path metaDataPath = getMetaDataPath(context.getContentId());
		Files.deleteIfExists(contenPath);
		Files.deleteIfExists(metaDataPath);
		Files.deleteIfExists(contenPath.getParent());
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
	public void readAttachment(AttachmentReadEventContext context) {
		logger.info("FS Attachment Service handler called for reading attachment with document id: {}",
				context.getContentId());

		try {
			InputStream fileInputStream = Files.newInputStream(getContentPath(context.getContentId()));
			context.getData().setContent(fileInputStream);
			context.setCompleted();
		} catch (IOException e) {
			throw new ServiceException(e);
		}
	}

	private Path getContentPath(String contentId) {
		return this.rootFolder.resolve("%s/content.bin".formatted(contentId));
	}

	private Path getMetaDataPath(String contentId) {
		return this.rootFolder.resolve("%s/metadata.json".formatted(contentId));
	}

}

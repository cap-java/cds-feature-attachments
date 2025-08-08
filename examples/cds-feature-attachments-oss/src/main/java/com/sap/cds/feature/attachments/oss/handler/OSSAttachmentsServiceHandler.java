/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.oss.handler;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.oss.client.AWSClient;
import com.sap.cds.feature.attachments.oss.client.AzureClient;
import com.sap.cds.feature.attachments.oss.client.MockOSClient;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * This class is an event handler that is called when an attachment is created, marked as deleted, restored or read.
 */
@ServiceName(value = "*", type = AttachmentService.class)
public class OSSAttachmentsServiceHandler implements EventHandler {

	private static final Logger logger = LoggerFactory.getLogger(OSSAttachmentsServiceHandler.class);
	private static OSClient osClient;
	/**
	 * Creates a new OSSAttachmentsServiceHandler with the given root folder.
	 * 
	 * @param rootFolder the root folder where the attachments are stored
	 * @throws IOException if the root folder cannot be created
	 */
	@SuppressWarnings("static-access")
	public OSSAttachmentsServiceHandler(Optional<ServiceBinding> bindingOpt) {
		if (!bindingOpt.isPresent()) {
			logger.error("No service binding found, hence the attachment service is not connected!");
			this.osClient = new MockOSClient();
			return;
		}
		String host = (String) bindingOpt.get().getCredentials().get("host");
		String containerUri = (String) bindingOpt.get().getCredentials().get("container_uri");
		if (host != null && java.util.stream.Stream.of("aws", "s3", "amazon").anyMatch(s -> host.contains(s))) {
			this.osClient = new AWSClient(bindingOpt.get());
		} else if (containerUri != null && java.util.stream.Stream.of("azure", "windows").anyMatch(s -> containerUri.contains(s))) {
			// Todo: catch error here in case containerUri is not valid
			this.osClient = new AzureClient(bindingOpt.get());
		} /*else if (java.util.stream.Stream.of("gcp", "google").anyMatch(s -> host.contains(s))) {
			osClient = new GCPClient(bindingOpt);
		} else {
			logger.warn("No valid service binding found for host " + host + ", hence the attachment service is not connected!");
			osClient = new MockOSClient();
		}*/
	}

	@On
	void createAttachment(AttachmentCreateEventContext context) throws IOException {
		logger.info("OS Attachment Service handler called for creating attachment for entity name: {}",
				context.getAttachmentEntity().getQualifiedName());

		String contentId = (String) context.getAttachmentIds().get(Attachments.ID);

		MediaData data = context.getData();

		osClient.uploadContent(data.getContent(), contentId, data.getMimeType())
				.thenRun(() -> {
					logger.info("Upload future is done: {}", context.getAttachmentEntity().getQualifiedName());
					data.setStatus(StatusCode.CLEAN);
					context.setIsInternalStored(false);
					context.setContentId(contentId);
					context.setCompleted();
				}).exceptionally(ex -> {
					logger.error("Upload failed for entity {}: {}", context.getAttachmentEntity().getQualifiedName(), ex.getMessage(), ex);
					return null;
				}).join();
	}

	@On
	void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws IOException {
		logger.info("OS Attachment Service handler called for marking attachment as deleted with document id: {}", context.getContentId());

		osClient.deleteContent(context.getContentId())
				.thenRun(() -> {context.setCompleted();})
				.join();
	}

	@On
	void restoreAttachment(AttachmentRestoreEventContext context) {
		logger.info("OS Attachment Service handler called for restoring attachment for timestamp: {}",
				context.getRestoreTimestamp());

		// nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

	@On
	void readAttachment(AttachmentReadEventContext context) throws IOException {
		logger.info("OS Attachment Service handler called for reading attachment with document id: {}",
				context.getContentId());
		osClient.readContent(context.getContentId())
			.whenComplete((inputStream, throwable) -> {
				if (throwable != null) {
					throw new RuntimeException("Failed to read content for id: " + context.getContentId(), throwable);
				} else if (inputStream != null) {
					context.getData().setContent(inputStream);
					context.getData().setStatus(StatusCode.CLEAN); //todo: malware scan staus?
					context.setCompleted();
				} else {
					throw new RuntimeException("Content not found for id: " + context.getContentId(), throwable);
				}
			}).join();
	}

}

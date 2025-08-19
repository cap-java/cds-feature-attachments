/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.oss.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.oss.client.AWSClient;
import com.sap.cds.feature.attachments.oss.client.AzureClient;
import com.sap.cds.feature.attachments.oss.client.GoogleClient;
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
	private final OSClient osClient;
	/**
     * Creates a new OSSAttachmentsServiceHandler using the provided {@link ServiceBinding}.
     * <p>
     * The handler will automatically detect the storage backend (AWS S3, Azure Blob Storage, Google Cloud Storage)
     * based on the credentials in the service binding. If no valid binding is found, a {@link MockOSClient} is used as fallback.
     * <ul>
     *   <li>For AWS, the binding must contain a "host" with "aws", "s3", or "amazon".</li>
     *   <li>For Azure, the binding must contain a "container_uri" with "azure" or "windows".</li>
     *   <li>For Google, the binding must contain a valid "base64EncodedPrivateKeyData" containing "google" or "gcp".</li>
     * </ul>
     * 
     * @param bindingOpt the optional {@link ServiceBinding} containing credentials for the object store service
     */

	 public OSSAttachmentsServiceHandler(Optional<ServiceBinding> bindingOpt) {
		if (bindingOpt.isEmpty()) {
			logger.error("No service binding found, hence the attachment service is not connected!");
			this.osClient = new MockOSClient();
			return;
		}
		ServiceBinding binding = bindingOpt.get();

		final String host = (String) binding.getCredentials().get("host"); //AWS
		final String containerUri = (String) binding.getCredentials().get("container_uri"); //Azure
		final String base64EncodedPrivateKeyData = (String) binding.getCredentials().get("base64EncodedPrivateKeyData"); //GCP

		// In the follwing, we check the service binding credentials to determine which client to use.
		// We do *not* throw exceptions here, as we want to provide a fallback to the MockOSClient if no valid service binding is found.
		// Then the rest of the application still works, but without actual attachment storage.
		if (host != null && java.util.stream.Stream.of("aws", "s3", "amazon").anyMatch(s -> host.contains(s))) {
			this.osClient = new AWSClient(binding);
		} else if (containerUri != null && java.util.stream.Stream.of("azure", "windows").anyMatch(s -> containerUri.contains(s))) {
			this.osClient = new AzureClient(binding);
		} else if (base64EncodedPrivateKeyData != null) {
		    String decoded = "";
			try {
				decoded = new String(java.util.Base64.getDecoder().decode(base64EncodedPrivateKeyData),java.nio.charset.StandardCharsets.UTF_8);
			} catch (IllegalArgumentException e) {
				logger.error("No valid base64EncodedPrivateKeyData found in Google service binding {}, hence the attachment service is not connected!", binding);
			}
			// Redeclaring is needed here to make the variable effectively final for the lambda expression
			final String dec = decoded;
			if (java.util.stream.Stream.of("google", "gcp").anyMatch(s -> dec.contains(s))) {
				this.osClient = new GoogleClient(binding);
			} else {
				logger.error("No valid Google service binding found in binding {}, hence the attachment service is not connected!", binding);
				this.osClient = new MockOSClient();
			}
		} else {
			logger.error("No valid service found in binding {}, hence the attachment service is not connected!", binding);
			this.osClient = new MockOSClient();
		}
	}

	@On
	void createAttachment(AttachmentCreateEventContext context) throws InterruptedException, ExecutionException {
		String fileName = context.getAttachmentEntity().getQualifiedName();
		logger.info("OS Attachment Service handler called for creating attachment for entity {}", fileName);
		
		String contentId = (String) context.getAttachmentIds().get(Attachments.ID);
		MediaData data = context.getData();

		try {
			osClient.uploadContent(data.getContent(), contentId, data.getMimeType()).get();
			logger.info("Uploaded file {}", fileName);
			context.setIsInternalStored(false);
			context.setContentId(contentId);
			context.setCompleted();
		} catch (ObjectStoreServiceException ex) {
			logger.error("Failed to upload file {}", fileName, ex);
			context.setCompleted();
			throw ex;
		}
	}

	@On
	void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws InterruptedException, ExecutionException {
		logger.info("OS Attachment Service handler called for marking attachment as deleted with document id {}", context.getContentId());

		try {
			osClient.deleteContent(context.getContentId()).get();
			context.setCompleted();
		} catch (ObjectStoreServiceException ex) {
			logger.error("Failed to delete file with document id {}", context.getContentId(), ex);
			context.setCompleted();
			throw ex;
		}
	}

	@On
	void restoreAttachment(AttachmentRestoreEventContext context) {
		logger.info("OS Attachment Service handler called for restoring attachment for timestamp: {}",
				context.getRestoreTimestamp());

		// nothing to do as data are stored in the database and handled by the database
		context.setCompleted();
	}

	@On
	void readAttachment(AttachmentReadEventContext context) throws InterruptedException, ExecutionException {
		logger.info("OS Attachment Service handler called for reading attachment with document id: {}",
				context.getContentId());
		try {
			Future<InputStream> future = osClient.readContent(context.getContentId());
			InputStream inputStream = future.get(); // Wait for the content to be read
			if (inputStream != null) {
				context.getData().setContent(inputStream);
			} else {
				logger.error("Document not found for id {}", context.getContentId());
				context.getData().setContent(new ByteArrayInputStream(new byte[0]));
			}
			context.setCompleted();
		} catch (ObjectStoreServiceException ex) {
			logger.error("Failed to read file with document id {}", context.getContentId(), ex);
			context.getData().setContent(new ByteArrayInputStream(new byte[0]));
			context.setCompleted();
			throw ex;
		}
	}

}

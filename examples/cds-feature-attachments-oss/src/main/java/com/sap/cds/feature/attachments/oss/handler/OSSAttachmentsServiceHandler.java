/**************************************************************************
 * (C) 2019-2024 SAP SE or an SAP affiliate company. All rights reserved. *
 **************************************************************************/
package com.sap.cds.feature.attachments.oss.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.oss.client.AWSClient;
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
		if (java.util.stream.Stream.of("aws", "s3", "amazon").anyMatch(s -> host.contains(s))) {
			this.osClient = new AWSClient(bindingOpt.get());
		} /*else if (java.util.stream.Stream.of("azure", "microsoft").anyMatch(s -> host.contains(s))) {
			osClient = new AzureClient(bindingOpt);
		} else if (java.util.stream.Stream.of("gcp", "google").anyMatch(s -> host.contains(s))) {
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
		data.setStatus(StatusCode.CLEAN);
		String completeFileName = contentId;
		try (InputStream input = data.getContent()) {
			context.setIsInternalStored(false);
			context.setContentId(completeFileName);
			osClient.uploadContent(input, completeFileName);
			context.setCompleted();
		}
	}

	@On
	void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) throws IOException {
		logger.info("OS Attachment Service handler called for marking attachment as deleted with document id: {}", context.getContentId());

		osClient.deleteContent(context.getContentId());
		context.setCompleted();
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
		context.getData().setContent(osClient.readContent(context.getContentId()));
		context.setCompleted();
	}

}

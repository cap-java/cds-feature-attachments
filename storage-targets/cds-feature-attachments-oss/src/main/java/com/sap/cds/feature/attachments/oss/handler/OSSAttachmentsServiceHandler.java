/*
 * © 2019-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.oss.client.AWSClient;
import com.sap.cds.feature.attachments.oss.client.AzureClient;
import com.sap.cds.feature.attachments.oss.client.GoogleClient;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an event handler that is called when an attachment is created, marked as deleted,
 * restored or read.
 */
@ServiceName(value = "*", type = AttachmentService.class)
public class OSSAttachmentsServiceHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(OSSAttachmentsServiceHandler.class);
  private final OSClient osClient;

  /**
   * Creates a new OSSAttachmentsServiceHandler using the provided {@link ServiceBinding}.
   *
   * <p>The handler will automatically detect the storage backend (AWS S3, Azure Blob Storage,
   * Google Cloud Storage) based on the credentials in the service binding. If no valid binding is
   * found, an {@link ObjectStoreServiceException} is thrown.
   *
   * <ul>
   *   <li>For AWS, the binding must contain a "host" with "aws", "s3", or "amazon".
   *   <li>For Azure, the binding must contain a "container_uri" with "azure" or "windows".
   *   <li>For Google, the binding must contain a valid "base64EncodedPrivateKeyData" containing
   *       "google" or "gcp".
   * </ul>
   *
   * @param binding the {@link ServiceBinding} containing credentials for the object store service
   * @throws ObjectStoreServiceException if no valid object store service binding is found
   */
  public OSSAttachmentsServiceHandler(ServiceBinding binding, ExecutorService executor) {
    final String host = (String) binding.getCredentials().get("host"); // AWS
    final String containerUri = (String) binding.getCredentials().get("container_uri"); // Azure
    final String base64EncodedPrivateKeyData =
        (String) binding.getCredentials().get("base64EncodedPrivateKeyData"); // GCP

    // Check the service binding credentials to determine which client to use.
    if (host != null && Stream.of("aws", "s3", "amazon").anyMatch(host::contains)) {
      this.osClient = new AWSClient(binding, executor);
    } else if (containerUri != null
        && Stream.of("azure", "windows").anyMatch(containerUri::contains)) {
      this.osClient = new AzureClient(binding, executor);
    } else if (base64EncodedPrivateKeyData != null) {
      String decoded = "";
      try {
        decoded =
            new String(
                Base64.getDecoder().decode(base64EncodedPrivateKeyData), StandardCharsets.UTF_8);
      } catch (IllegalArgumentException e) {
        throw new ObjectStoreServiceException(
            "No valid base64EncodedPrivateKeyData found in Google service binding: %s"
                .formatted(binding),
            e);
      }
      // Redeclaring is needed here to make the variable effectively final for the lambda expression
      final String dec = decoded;
      if (Stream.of("google", "gcp").anyMatch(dec::contains)) {
        this.osClient = new GoogleClient(binding, executor);
      } else {
        throw new ObjectStoreServiceException(
            "No valid Google service binding found in binding: %s".formatted(binding));
      }
    } else {
      throw new ObjectStoreServiceException(
          "No valid object store service found in binding: %s. Please ensure you have a valid AWS S3, Azure Blob Storage, or Google Cloud Storage service binding."
              .formatted(binding));
    }
  }

  @On
  void createAttachment(AttachmentCreateEventContext context) {
    logger.info(
        "OS Attachment Service handler called for creating attachment for entity {}",
        context.getAttachmentEntity().getQualifiedName());

    String contentId = (String) context.getAttachmentIds().get(Attachments.ID);
    MediaData data = context.getData();
    String fileName = data.getFileName();

    try {
      osClient.uploadContent(data.getContent(), contentId, data.getMimeType()).get();
      logger.info("Uploaded file {}", fileName);
      context.getData().setStatus(StatusCode.SCANNING);
      context.setIsInternalStored(false);
      context.setContentId(contentId);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ServiceException("Failed to upload file {}", fileName, ex);
    } catch (ObjectStoreServiceException | ExecutionException ex) {
      throw new ServiceException("Failed to upload file {}", fileName, ex);
    } finally {
      context.setCompleted();
    }
  }

  @On
  void markAttachmentAsDeleted(AttachmentMarkAsDeletedEventContext context) {
    logger.info(
        "OS Attachment Service handler called for marking attachment as deleted with document id {}",
        context.getContentId());

    try {
      osClient.deleteContent(context.getContentId()).get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ServiceException(
          "Failed to delete file with document id {}", context.getContentId(), ex);
    } catch (ObjectStoreServiceException | ExecutionException ex) {
      throw new ServiceException(
          "Failed to delete file with document id {}", context.getContentId(), ex);
    } finally {
      context.setCompleted();
    }
  }

  @On
  void restoreAttachment(AttachmentRestoreEventContext context) {
    logger.info(
        "OS Attachment Service handler called for restoring attachment for timestamp: {}",
        context.getRestoreTimestamp());

    // nothing to do as data are stored in the database and handled by the database
    context.setCompleted();
  }

  @On
  void readAttachment(AttachmentReadEventContext context) {
    logger.info(
        "OS Attachment Service handler called for reading attachment with document id: {}",
        context.getContentId());
    try {
      Future<InputStream> future = osClient.readContent(context.getContentId());
      InputStream inputStream = future.get(); // Wait for the content to be read
      if (inputStream != null) {
        context.getData().setContent(inputStream);
      } else {
        logger.error("Document not found for id {}", context.getContentId());
        throw new AttachmentNotFoundException(
            "Document not found for id " + context.getContentId(), context.getContentId());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ServiceException(
          "Failed to read file with document id {}", context.getContentId(), ex);
    } catch (AttachmentNotFoundException ex) {
      throw new ServiceException(
          "Attachment not found with document id {}", context.getContentId(), ex);
    } catch (ObjectStoreServiceException | ExecutionException ex) {
      throw new ServiceException(
          "Failed to read file with document id {}", context.getContentId(), ex);
    } finally {
      context.setCompleted();
    }
  }
}

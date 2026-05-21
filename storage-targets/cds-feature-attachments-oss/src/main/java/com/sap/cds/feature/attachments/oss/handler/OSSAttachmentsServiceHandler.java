/*
 * © 2019-2024 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.oss.handler;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.MediaData;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.oss.client.OSClient;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentCreateEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentMarkAsDeletedEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentReadEventContext;
import com.sap.cds.feature.attachments.service.model.servicehandler.AttachmentRestoreEventContext;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
  private final boolean multitenancyEnabled;
  private final String objectStoreKind;

  /**
   * Creates a new OSSAttachmentsServiceHandler with the given {@link OSClient}.
   *
   * <p>Use {@link com.sap.cds.feature.attachments.oss.client.OSClientFactory#create
   * OSClientFactory.create()} to obtain an {@link OSClient} from a service binding.
   *
   * @param osClient the object store client for storage operations
   * @param multitenancyEnabled whether multitenancy is enabled
   * @param objectStoreKind the object store kind (e.g. "shared")
   */
  public OSSAttachmentsServiceHandler(
      OSClient osClient, boolean multitenancyEnabled, String objectStoreKind) {
    this.osClient = osClient;
    this.multitenancyEnabled = multitenancyEnabled;
    this.objectStoreKind = objectStoreKind;
  }

  @On
  void createAttachment(AttachmentCreateEventContext context) {
    logger.info(
        "OS Attachment Service handler called for creating attachment for entity {}",
        context.getAttachmentEntity().getQualifiedName());

    String contentId = (String) context.getAttachmentIds().get(Attachments.ID);
    MediaData data = context.getData();
    String fileName = data.getFileName();
    String objectKey = buildObjectKey(context, contentId);

    try {
      osClient.uploadContent(data.getContent(), objectKey, data.getMimeType()).get();
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
        "OS Attachment Service handler called for marking attachment as deleted with document id"
            + " {}",
        context.getContentId());

    try {
      String objectKey = buildObjectKey(context, context.getContentId());
      osClient.deleteContent(objectKey).get();
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
      String objectKey = buildObjectKey(context, context.getContentId());
      Future<InputStream> future = osClient.readContent(objectKey);
      InputStream inputStream = future.get(); // Wait for the content to be read
      if (inputStream != null) {
        context.getData().setContent(inputStream);
      } else {
        logger.error("Document not found for id {}", context.getContentId());
        throw new ServiceException(
            "Document not found for id " + context.getContentId(), context.getContentId());
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ServiceException(
          "Failed to read file with document id {}", context.getContentId(), ex);
    } catch (ObjectStoreServiceException | ExecutionException ex) {
      throw new ServiceException(
          "Failed to read file with document id {}", context.getContentId(), ex);
    } finally {
      context.setCompleted();
    }
  }

  /**
   * Builds the object key for storage operations. In shared multitenancy mode, the key is prefixed
   * with the tenant ID ({@code tenantId/contentId}). Otherwise, the raw content ID is used.
   */
  private String buildObjectKey(EventContext context, String contentId) {
    if (multitenancyEnabled && "shared".equals(objectStoreKind)) {
      String tenant = getTenant(context);
      validateTenantId(tenant);
      validateContentId(contentId);
      return tenant + "/" + contentId;
    }
    return contentId;
  }

  private String getTenant(EventContext context) {
    String tenant = context.getUserInfo().getTenant();
    if (tenant == null) {
      throw new ServiceException("Tenant ID is required for multitenant attachment operations");
    }
    return tenant;
  }

  /**
   * Validates that the tenant ID is safe for use in object key construction. Rejects null, empty,
   * or values containing path separators ({@code /}, {@code \}, {@code ..}) to prevent path
   * traversal attacks.
   *
   * @param tenantId the tenant ID to validate
   * @throws ServiceException if the tenant ID is invalid
   */
  static void validateTenantId(String tenantId) {
    if (tenantId == null
        || tenantId.isEmpty()
        || tenantId.contains("/")
        || tenantId.contains("\\")
        || tenantId.contains("..")) {
      throw new ServiceException(
          "Invalid tenant ID for attachment storage: must not be empty or contain path separators");
    }
  }

  private static void validateContentId(String contentId) {
    if (contentId == null
        || contentId.isEmpty()
        || contentId.contains("/")
        || contentId.contains("\\")
        || contentId.contains("..")) {
      throw new ServiceException(
          "Invalid content ID for attachment storage: must not be empty or contain path"
              + " separators");
    }
  }
}

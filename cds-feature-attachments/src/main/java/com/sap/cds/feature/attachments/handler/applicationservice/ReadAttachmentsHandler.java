/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Converter;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.StatusCode;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.AttachmentStatusValidator;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.BeforeReadItemsModifier;
import com.sap.cds.feature.attachments.handler.applicationservice.readhelper.LazyProxyInputStream;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.feature.attachments.handler.common.AssociationCascader;
import com.sap.cds.feature.attachments.service.AttachmentService;
import com.sap.cds.feature.attachments.service.malware.AsyncMalwareScanExecutor;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link ReadAttachmentsHandler} is an event handler that is responsible for reading
 * attachments for entities. In the before read event, it modifies the CQN to include the content
 * ID, status and scanned-at timestamp. In the after read event, it adds a proxy for the stream of
 * the attachments service to the data. Only if the data are read the proxy forwards the request to
 * the attachment service to read the attachment. This is needed to have a filled stream in the data
 * to enable the OData V4 adapter to enrich the data that a link to the content can be shown on the
 * UI.
 *
 * <p>Additionally, this handler implements rescan-on-download: if an attachment's last scan is
 * older than {@link #RESCAN_THRESHOLD}, the handler transitions the attachment status to {@code
 * SCANNING}, triggers an asynchronous malware rescan, and rejects the current download with a "not
 * scanned" error. The client must retry the download after the rescan completes.
 */
@ServiceName(value = "*", type = ApplicationService.class)
public class ReadAttachmentsHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ReadAttachmentsHandler.class);

  /**
   * The duration after which a previously scanned attachment is considered stale and must be
   * rescanned on download, as recommended by the SAP Malware Scanning Service FAQ.
   */
  static final Duration RESCAN_THRESHOLD = Duration.ofDays(3);

  private final AttachmentService attachmentService;
  private final AttachmentStatusValidator statusValidator;
  private final AsyncMalwareScanExecutor scanExecutor;
  private final PersistenceService persistenceService;
  private final AssociationCascader cascader;
  private final boolean scannerAvailable;

  public ReadAttachmentsHandler(
      AttachmentService attachmentService,
      AttachmentStatusValidator statusValidator,
      AsyncMalwareScanExecutor scanExecutor,
      PersistenceService persistenceService,
      AssociationCascader cascader,
      boolean scannerAvailable) {
    this.attachmentService =
        requireNonNull(attachmentService, "attachmentService must not be null");
    this.statusValidator = requireNonNull(statusValidator, "statusValidator must not be null");
    this.scanExecutor = requireNonNull(scanExecutor, "scanExecutor must not be null");
    this.persistenceService =
        requireNonNull(persistenceService, "persistenceService must not be null");
    this.cascader = requireNonNull(cascader, "cascader must not be null");
    this.scannerAvailable = scannerAvailable;
  }

  @Before
  @HandlerOrder(HandlerOrder.EARLY)
  void processBefore(CdsReadEventContext context) {
    logger.debug("Processing before {} for entity {}.", context.getEvent(), context.getTarget());

    CdsModel cdsModel = context.getModel();
    List<String> fieldNames = cascader.findMediaAssociationNames(cdsModel, context.getTarget());
    List<String> inlinePrefixes =
        ApplicationHandlerHelper.getInlineAttachmentFieldNames(context.getTarget());
    if (!fieldNames.isEmpty() || !inlinePrefixes.isEmpty()) {
      CqnSelect resultCqn =
          CQL.copy(context.getCqn(), new BeforeReadItemsModifier(fieldNames, inlinePrefixes));
      context.setCqn(resultCqn);
    }
  }

  @After
  @HandlerOrder(HandlerOrder.EARLY)
  void processAfter(CdsReadEventContext context, List<CdsData> data) {
    if (ApplicationHandlerHelper.containsContentField(context.getTarget(), data)) {
      logger.debug(
          "Processing after {} event for entity {}", context.getEvent(), context.getTarget());

      Converter converter =
          (path, element, value) -> {
            Attachments attachment;
            // Check if this is an inline attachment field
            Optional<String> inlinePrefix =
                ApplicationHandlerHelper.getInlineAttachmentPrefix(
                    path.target().type(), element.getName());
            if (inlinePrefix.isPresent()) {
              attachment =
                  ApplicationHandlerHelper.extractInlineAttachment(
                      path.target().values(), inlinePrefix.get());
            } else {
              attachment = Attachments.of(path.target().values());
            }
            InputStream content = attachment.getContent();
            if (nonNull(attachment.getContentId())) {
              verifyStatus(path, attachment);
              Supplier<InputStream> supplier =
                  nonNull(content)
                      ? () -> content
                      : () -> attachmentService.readAttachment(attachment.getContentId());
              return new LazyProxyInputStream(supplier, statusValidator, attachment.getStatus());
            } else {
              return value;
            }
          };

      CdsDataProcessor.create()
          .addConverter(ApplicationHandlerHelper.MEDIA_CONTENT_FILTER, converter)
          .process(data, context.getTarget());
    }
  }

  private void verifyStatus(Path path, Attachments attachment) {
    Optional<String> inlinePrefix =
        Optional.ofNullable((String) attachment.get(ApplicationHandlerHelper.INLINE_PREFIX_MARKER));
    if (areKeysEmpty(path.target().keys())) {
      String currentStatus = attachment.getStatus();
      logger.debug(
          "In verify status for content id {} and status {}",
          attachment.getContentId(),
          currentStatus);
      if (scannerAvailable && needsScan(currentStatus, attachment.getScannedAt())) {
        if (StatusCode.CLEAN.equals(currentStatus)) {
          transitionToScanning(path.target().entity(), attachment, inlinePrefix);
        }
        logger.debug(
            "Scanning content with ID {} for malware, has current status {}",
            attachment.getContentId(),
            currentStatus);
        scanExecutor.scanAsync(path.target().entity(), attachment.getContentId(), inlinePrefix);
      }
      statusValidator.verifyStatus(attachment.getStatus());
    }
  }

  private boolean needsScan(String status, Instant scannedAt) {
    if (StatusCode.UNSCANNED.equals(status)
        || StatusCode.SCANNING.equals(status)
        || status == null) {
      return true;
    }
    return StatusCode.CLEAN.equals(status) && isScanStale(scannedAt);
  }

  private boolean isScanStale(Instant scannedAt) {
    return scannedAt == null || Instant.now().isAfter(scannedAt.plus(RESCAN_THRESHOLD));
  }

  private void transitionToScanning(
      CdsEntity entity, Attachments attachment, Optional<String> inlinePrefix) {
    logger.debug(
        "Attachment {} has stale scan (scannedAt={}), transitioning to SCANNING for rescan.",
        attachment.getContentId(),
        attachment.getScannedAt());

    String contentIdCol = resolveColumn(Attachments.CONTENT_ID, inlinePrefix);
    String statusCol = resolveColumn(Attachments.STATUS, inlinePrefix);

    Attachments updateData = Attachments.create();
    updateData.put(statusCol, StatusCode.SCANNING);

    // Filter by contentId because primary keys are unavailable during content-only reads
    // (areKeysEmpty returns true). This is consistent with DefaultAttachmentMalwareScanner.
    CqnUpdate update =
        Update.entity(entity)
            .data(updateData)
            .where(entry -> entry.get(contentIdCol).eq(attachment.getContentId()));
    persistenceService.run(update);

    attachment.setStatus(StatusCode.SCANNING);
  }

  private static String resolveColumn(String fieldName, Optional<String> inlinePrefix) {
    return inlinePrefix.map(p -> p + "_" + fieldName).orElse(fieldName);
  }

  private boolean areKeysEmpty(Map<String, Object> keys) {
    return keys.values().stream().allMatch(Objects::isNull);
  }
}

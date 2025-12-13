/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class {@link LazyProxyInputStream} is a buffered proxy for an
 * {@link InputStream}. The class validates the attachment status and
 * stream health before making the stream available for reading.
 * For content requests, the stream is eagerly initialized to catch errors before serialization.
 * For metadata requests, the stream is lazily initialized only when accessed.
 */
public final class LazyProxyInputStream extends InputStream {
  private static final Logger logger = LoggerFactory.getLogger(LazyProxyInputStream.class);

  private final Supplier<InputStream> inputStreamSupplier;
  private final AttachmentStatusValidator attachmentStatusValidator;
  private final String status;
  private InputStream delegate;

  public LazyProxyInputStream(
      Supplier<InputStream> inputStreamSupplier,
      AttachmentStatusValidator attachmentStatusValidator,
      String status,
      boolean eagerValidation) throws IOException {
    this.inputStreamSupplier = inputStreamSupplier;
    this.attachmentStatusValidator = attachmentStatusValidator;
    this.status = status;
    
    // Only eagerly initialize if this is a content request
    if (eagerValidation) {
      logger.debug("Eagerly initializing stream for content request");
      this.delegate = initializeDelegate();
    }
  }

  @Override
  public int read() throws IOException {
    logger.debug("Reading from input stream");
    return getDelegate().read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    logger.debug("Reading byte from input stream");
    return getDelegate().read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    logger.debug("Reading byte with off and len from input stream");
    return getDelegate().read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    logger.debug("Closing input stream");
    if (delegate != null) {
      delegate.close();
    }
  }

  private InputStream getDelegate() throws IOException {
    if (delegate == null) {
      logger.debug("Lazily initializing stream for metadata request");
      delegate = initializeDelegate();
    }
    return delegate;
  }

  private InputStream initializeDelegate() throws IOException {
    logger.debug("Initializing buffered input stream");
    
    // Validate status before attempting to create the stream
    attachmentStatusValidator.verifyStatus(status);
    
    // Get the input stream from the supplier
    // ServiceExceptions should propagate as-is to preserve error details
    InputStream rawStream = inputStreamSupplier.get();
    
    // Wrap in BufferedInputStream to enable mark/reset and buffering
    InputStream bufferedStream = new BufferedInputStream(rawStream, 1024);
    
    // Verify stream health by reading the first byte
    bufferedStream.mark(1024);
    int firstByte = bufferedStream.read();
    
    if (firstByte == -1) {
      logger.error("Attachment content is empty");
      throw new IOException("Attachment content is empty");
    }
    
    // Reset to the beginning so consumer gets the full stream
    bufferedStream.reset();
    logger.debug("Buffered input stream initialized and verified");
    
    return bufferedStream;
  }
}

/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import com.sap.cds.services.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * An InputStream wrapper that tracks bytes read and enforces a maximum size limit. This allows for
 * memory-efficient streaming validation without buffering entire files.
 */
public final class CountingInputStream extends InputStream {

  private final InputStream delegate;
  private final long maxBytes;
  private long bytesRead = 0;

  /**
   * Creates a CountingInputStream that enforces a maximum size limit.
   *
   * @param delegate the underlying InputStream to wrap
   * @param maxBytes the maximum number of bytes allowed to be read
   * @throws IllegalArgumentException if maxBytes is negative
   */
  public CountingInputStream(InputStream delegate, long maxBytes) {
    if (maxBytes < 0) {
      throw new IllegalArgumentException("maxBytes must be non-negative");
    }
    this.delegate = delegate;
    this.maxBytes = maxBytes;
  }

  @Override
  public int read() throws IOException {
    int b = delegate.read();
    if (b != -1) {
      checkLimit(1);
    }
    return b;
  }

  @Override
  public int read(byte[] b) throws IOException {
    int result = delegate.read(b);
    if (result > 0) {
      checkLimit(result);
    }
    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int result = delegate.read(b, off, len);
    if (result > 0) {
      checkLimit(result);
    }
    return result;
  }

  @Override
  public long skip(long n) throws IOException {
    long skipped = delegate.skip(n);
    if (skipped > 0) {
      checkLimit(skipped);
    }
    return skipped;
  }

  @Override
  public int available() throws IOException {
    return delegate.available();
  }

  @Override
  public void close() throws IOException {
    if (delegate != null) {
      delegate.close();
    }
  }

  @Override
  public void mark(int readlimit) {
    delegate.mark(readlimit);
  }

  @Override
  public void reset() throws IOException {
    delegate.reset();
  }

  @Override
  public boolean markSupported() {
    return delegate.markSupported();
  }

  /**
   * Returns the number of bytes that have been read so far.
   *
   * @return the total bytes read
   */
  public long getBytesRead() {
    return bytesRead;
  }

  /**
   * Checks if adding the specified number of bytes would exceed the limit. Increments the bytesRead
   * counter and throws an exception if limit is exceeded.
   *
   * @param bytes the number of bytes being read
   * @throws ServiceException if reading these bytes would exceed maxBytes
   */
  private void checkLimit(long bytes) {
    bytesRead += bytes;
    if (bytesRead > maxBytes) {
      throw new UncheckedIOException(
          new IOException(String.format("File size exceeds the limit of %d bytes", maxBytes)));
    }
  }
}

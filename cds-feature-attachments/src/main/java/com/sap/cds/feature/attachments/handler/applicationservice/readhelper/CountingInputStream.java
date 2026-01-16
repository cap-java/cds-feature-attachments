/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.readhelper;

import com.sap.cds.feature.attachments.handler.applicationservice.helper.ExtendedErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends InputStream {

    private final InputStream delegate;
    private long byteCount = 0;
    private long maxBytes;

    public CountingInputStream(InputStream delegate, long maxBytes) {
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
        int bytesRead = delegate.read(b);
        if (bytesRead > 0) {
            checkLimit(bytesRead);
        }
        return bytesRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = delegate.read(b, off, len);
        if (bytesRead > 0) {
            checkLimit(bytesRead);
        }
        return bytesRead;
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
    public void close() throws IOException {
        if (delegate != null)
            delegate.close();
    }

    private void checkLimit(long bytes) {
        byteCount += bytes;
        if (byteCount > maxBytes) {
            throw new ServiceException(
                    ExtendedErrorStatuses.CONTENT_TOO_LARGE,
                    "AttachmentSizeExceeded",
                    maxBytes);
        }
    }
}

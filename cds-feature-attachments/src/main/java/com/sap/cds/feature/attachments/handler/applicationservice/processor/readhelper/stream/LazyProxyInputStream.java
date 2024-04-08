package com.sap.cds.feature.attachments.handler.applicationservice.processor.readhelper.stream;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.services.ServiceException;

/**
	* The class {@link LazyProxyInputStream} is a lazy proxy for an {@link InputStream}.
	* The class is used to create a proxy for an {@link InputStream} that is not yet available.
	*/
public class LazyProxyInputStream extends InputStream {
	private static final Logger logger = LoggerFactory.getLogger(LazyProxyInputStream.class);

	private final InputStreamSupplier inputStreamSupplier;
	private final String status;
	private InputStream delegate;


	public LazyProxyInputStream(InputStreamSupplier inputStreamSupplier, String status) {
		this.inputStreamSupplier = inputStreamSupplier;
		this.status = status;
	}

	private InputStream getDelegate() throws IOException {
		if (!StatusCode.CLEAN.equals(status)) {
			//TODO translation
			throw new ServiceException("Attachment is not clean");
		}

		if (delegate == null) {
			logger.debug("Creating delegate input stream");
			delegate = inputStreamSupplier.get();
		}
		return delegate;
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

	public interface InputStreamSupplier {
		InputStream get() throws IOException;
	}

}


package com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.model;

import java.io.IOException;
import java.io.InputStream;

import com.sap.cds.feature.attachments.generated.cds4j.com.sap.attachments.StatusCode;
import com.sap.cds.services.ServiceException;

/**
	* The class {@link LazyProxyInputStream} is a lazy proxy for an {@link InputStream}.
	* The class is used to create a proxy for an {@link InputStream} that is not yet available.
	*/
public class LazyProxyInputStream extends InputStream {
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
			delegate = inputStreamSupplier.get();
		}
		return delegate;
	}

	@Override
	public int read() throws IOException {
		return getDelegate().read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return getDelegate().read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return getDelegate().read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if (delegate != null) {
			delegate.close();
		}
	}

	public interface InputStreamSupplier {
		InputStream get() throws IOException;
	}

}


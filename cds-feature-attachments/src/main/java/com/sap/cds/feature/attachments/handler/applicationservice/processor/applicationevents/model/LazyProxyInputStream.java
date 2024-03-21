package com.sap.cds.feature.attachments.handler.applicationservice.processor.applicationevents.model;

import java.io.IOException;
import java.io.InputStream;

public class LazyProxyInputStream extends InputStream {
	private final InputStreamSupplier inputStreamSupplier;
	private InputStream delegate;

	public LazyProxyInputStream(InputStreamSupplier inputStreamSupplier) {
		this.inputStreamSupplier = inputStreamSupplier;
	}

	private InputStream getDelegate() throws IOException {
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


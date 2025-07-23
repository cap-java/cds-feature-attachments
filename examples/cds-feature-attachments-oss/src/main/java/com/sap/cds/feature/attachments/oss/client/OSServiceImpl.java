package com.sap.cds.feature.attachments.oss.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;

import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.services.ServiceDelegator;
import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

/**
 * Implementation of the {@link OSService} interface.
 * This class handles the readAttchment, createAttachment, markAttachmentAsDeleted,
 * and restoreAttachment methods, delegating the actual logic to the appropriate
 * service methods.
 */
public class OSServiceImpl extends ServiceDelegator implements OSService {

	private static OSClient osClient;

	public OSServiceImpl(ServiceBinding binding) {
		super(DEFAULT_NAME);
		// possibly select the right client based on the service binding
		osClient = new AWSClient(binding);
	}

	@Override
	public InputStream readAttachment(String contentId) {
		System.out.println("OSServiceImpl.readAttachment called with contentId: " + contentId);

		String dummyContent = "This is dummy input.";
        InputStream is = new ByteArrayInputStream(dummyContent.getBytes());
		return is;
	}

	@Override
	public AttachmentModificationResult createAttachment(InputStream input) {
		System.out.println("Called createAttachment with input: " + input);
		osClient.uploadContent(input);
		return new AttachmentModificationResult(true, "dummyContentId", "dummyStatus");
	}


	@Override
	public void markAttachmentAsDeleted(MarkAsDeletedInput input) {
        System.out.println("OSServiceImpl.markAttachmentAsDeleted called with input: " + input);
	}

	@Override
	public void restoreAttachment(Instant restoreTimestamp) {
        System.out.println("OSServiceImpl.restoreAttachment called");
	}

}

package com.sap.cds.feature.attachments.integrationtests.nondraftservice.helper;

import com.sap.cds.feature.attachments.generated.integration.test.cds4j.sap.attachments.Attachments;

public class AttachmentsBuilder {

	private Attachments attachment;

	private AttachmentsBuilder() {
		attachment = Attachments.create();
	}

	public static AttachmentsBuilder create() {
		return new AttachmentsBuilder();
	}

	public AttachmentsBuilder setMimeType(String mimeType) {
		attachment.setMimeType(mimeType);
		return this;
	}

	public AttachmentsBuilder setFileName(String fileName) {
		attachment.setFileName(fileName);
		return this;
	}

	public Attachments build() {
		return attachment;
	}

}
